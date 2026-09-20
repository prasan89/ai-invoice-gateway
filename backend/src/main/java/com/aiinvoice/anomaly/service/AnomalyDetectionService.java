package com.aiinvoice.anomaly.service;

import com.aiinvoice.anomaly.dto.AnomalyDto;
import com.aiinvoice.anomaly.dto.ReviewAnomalyRequest;
import com.aiinvoice.anomaly.entity.InvoiceAnomaly;
import com.aiinvoice.anomaly.entity.VendorBaseline;
import com.aiinvoice.anomaly.repository.InvoiceAnomalyRepository;
import com.aiinvoice.anomaly.repository.VendorBaselineRepository;
import com.aiinvoice.invoice.entity.Invoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final InvoiceAnomalyRepository anomalyRepo;
    private final VendorBaselineRepository baselineRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AnomalyDto detectAndSave(Invoice invoice) {
        List<String> types = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        int score = 0;

        VendorBaseline baseline = baselineRepo
            .findByOrganizationIdAndSupplierGstin(invoice.getOrganizationId(),
                invoice.getSupplierGstin() != null ? invoice.getSupplierGstin() : "__UNKNOWN__")
            .orElse(null);

        // NEW_VENDOR
        if (baseline == null || baseline.getInvoiceCount() < 3) {
            types.add("NEW_VENDOR");
            reasons.add("Vendor has fewer than 3 historical invoices — limited trust baseline.");
            score += 20;
        }

        // AMOUNT_SPIKE
        if (baseline != null && baseline.getAvgAmount() != null && invoice.getTotalAmount() != null) {
            BigDecimal avg = baseline.getAvgAmount();
            BigDecimal stddev = baseline.getStddevAmount() != null ? baseline.getStddevAmount() : avg.multiply(BigDecimal.valueOf(0.2));
            BigDecimal threshold = avg.add(stddev.multiply(BigDecimal.valueOf(3)));
            if (invoice.getTotalAmount().compareTo(threshold) > 0) {
                types.add("AMOUNT_SPIKE");
                reasons.add(String.format("Invoice amount ₹%.2f is more than 3σ above vendor average of ₹%.2f.",
                    invoice.getTotalAmount(), avg));
                score += 30;
            }
        }

        // GST_MISMATCH
        if (invoice.getTotalAmount() != null && invoice.getTaxAmount() != null
                && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRate = invoice.getTaxAmount()
                .divide(invoice.getTotalAmount().subtract(invoice.getTaxAmount()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            if (baseline != null && baseline.getTypicalGstRate() != null) {
                BigDecimal delta = effectiveRate.subtract(baseline.getTypicalGstRate()).abs();
                if (delta.compareTo(BigDecimal.valueOf(5)) > 0) {
                    types.add("GST_MISMATCH");
                    reasons.add(String.format("Effective GST rate %.1f%% deviates from vendor typical %.1f%%.",
                        effectiveRate.floatValue(), baseline.getTypicalGstRate().floatValue()));
                    score += 25;
                }
            }
        }

        // DUPLICATE_RISK
        if (invoice.getDuplicateScore() != null && invoice.getDuplicateScore() >= 70) {
            types.add("DUPLICATE_RISK");
            reasons.add(String.format("Duplicate score %d%% — likely duplicate or re-submission.", invoice.getDuplicateScore()));
            score += invoice.getDuplicateScore() >= 90 ? 40 : 25;
        }

        // ARITHMETIC_FAIL
        if ("FAIL".equals(invoice.getArithmeticStatus() != null ? invoice.getArithmeticStatus().name() : "")) {
            types.add("ARITHMETIC_FAIL");
            reasons.add("Invoice line totals do not reconcile with the stated invoice total.");
            score += 20;
        }

        // LOW_CONFIDENCE
        if (invoice.getExtractionConfidence() != null
                && invoice.getExtractionConfidence().compareTo(BigDecimal.valueOf(0.5)) < 0) {
            types.add("LOW_CONFIDENCE");
            reasons.add(String.format("AI extraction confidence %.0f%% — document quality may be poor.",
                invoice.getExtractionConfidence().multiply(BigDecimal.valueOf(100)).floatValue()));
            score += 15;
        }

        score = Math.min(score, 100);
        String level = score >= 75 ? "CRITICAL" : score >= 50 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";

        InvoiceAnomaly anomaly = anomalyRepo.findByInvoiceId(invoice.getId())
            .orElseGet(() -> { InvoiceAnomaly a = new InvoiceAnomaly(); a.setId(UUID.randomUUID()); return a; });
        anomaly.setOrganizationId(invoice.getOrganizationId());
        anomaly.setInvoiceId(invoice.getId());
        anomaly.setRiskScore(score);
        anomaly.setRiskLevel(level);
        anomaly.setAnomalyTypes(toJson(types));
        anomaly.setReasons(toJson(reasons));
        anomaly.setDetectedAt(Instant.now());
        if (baseline != null) anomaly.setVendorBaseline(toJson(Map.of(
            "avgAmount", baseline.getAvgAmount(), "invoiceCount", baseline.getInvoiceCount())));
        anomalyRepo.save(anomaly);

        // Update risk on invoice
        invoice.setRiskScore(score);
        invoice.setRiskLevel(level);

        updateBaseline(invoice);
        return toDto(anomaly);
    }

    @Transactional
    public AnomalyDto reviewAnomaly(UUID anomalyId, UUID reviewerId, ReviewAnomalyRequest req) {
        InvoiceAnomaly a = anomalyRepo.findById(anomalyId)
            .orElseThrow(() -> new NoSuchElementException("Anomaly not found"));
        a.setReviewedBy(reviewerId);
        a.setReviewedAt(Instant.now());
        a.setReviewOutcome(req.outcome());
        return toDto(anomalyRepo.save(a));
    }

    public List<AnomalyDto> listByOrg(UUID orgId) {
        return anomalyRepo.findByOrganizationIdOrderByDetectedAtDesc(orgId)
            .stream().map(this::toDto).toList();
    }

    public Optional<AnomalyDto> findByInvoice(UUID invoiceId) {
        return anomalyRepo.findByInvoiceId(invoiceId).map(this::toDto);
    }

    private void updateBaseline(Invoice inv) {
        if (inv.getSupplierGstin() == null) return;
        VendorBaseline b = baselineRepo
            .findByOrganizationIdAndSupplierGstin(inv.getOrganizationId(), inv.getSupplierGstin())
            .orElseGet(() -> {
                VendorBaseline nb = new VendorBaseline();
                nb.setId(UUID.randomUUID());
                nb.setOrganizationId(inv.getOrganizationId());
                nb.setSupplierGstin(inv.getSupplierGstin());
                nb.setFirstSeen(Instant.now());
                return nb;
            });

        b.setSupplierName(inv.getSupplierName());
        b.setLastSeen(Instant.now());
        b.setUpdatedAt(Instant.now());

        int n = b.getInvoiceCount() + 1;
        b.setInvoiceCount(n);

        if (inv.getTotalAmount() != null) {
            BigDecimal amt = inv.getTotalAmount();
            if (b.getMinAmount() == null || amt.compareTo(b.getMinAmount()) < 0) b.setMinAmount(amt);
            if (b.getMaxAmount() == null || amt.compareTo(b.getMaxAmount()) > 0) b.setMaxAmount(amt);

            if (b.getAvgAmount() == null) {
                b.setAvgAmount(amt);
                b.setStddevAmount(BigDecimal.ZERO);
            } else {
                // Welford online algorithm
                BigDecimal prevAvg = b.getAvgAmount();
                BigDecimal newAvg = prevAvg.add(amt.subtract(prevAvg)
                    .divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP));
                BigDecimal prevVar = b.getStddevAmount() != null
                    ? b.getStddevAmount().pow(2) : BigDecimal.ZERO;
                BigDecimal newVar = prevVar.add(
                    amt.subtract(prevAvg).multiply(amt.subtract(newAvg))
                        .divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP));
                b.setAvgAmount(newAvg.setScale(2, RoundingMode.HALF_UP));
                b.setStddevAmount(newVar.sqrt(new MathContext(10)).setScale(2, RoundingMode.HALF_UP));
            }
        }

        if (inv.getTaxAmount() != null && inv.getTotalAmount() != null
                && inv.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxable = inv.getTotalAmount().subtract(inv.getTaxAmount());
            if (taxable.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = inv.getTaxAmount()
                    .divide(taxable, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
                b.setTypicalGstRate(rate);
            }
        }

        baselineRepo.save(b);
    }

    private AnomalyDto toDto(InvoiceAnomaly a) {
        return new AnomalyDto(a.getId(), a.getInvoiceId(), a.getRiskScore(), a.getRiskLevel(),
            fromJson(a.getAnomalyTypes()), fromJson(a.getReasons()), a.getDetectedAt(), a.getReviewOutcome());
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null) return List.of();
        try { return objectMapper.readValue(json, List.class); } catch (Exception e) { return List.of(); }
    }
}
