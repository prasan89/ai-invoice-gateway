package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Vendor;
import com.aiinvoice.invoice.repository.VendorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class VendorService {

    private static final Pattern SUFFIX_PATTERN = Pattern.compile(
        "\\b(PVT|PRIVATE|LTD|LIMITED|LLP|INC|CORP|CO)\\b\\.?\\s*$",
        Pattern.CASE_INSENSITIVE);

    public record VendorMatchResult(
        Vendor vendor,
        boolean anomalyFlag,
        String riskTier,
        int anomalyCount,
        BigDecimal typicalGstRate
    ) {}

    private final VendorRepository repository;

    public VendorService(VendorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VendorMatchResult matchOrCreate(String gstin, String supplierName,
                                           BigDecimal invoiceTotal, BigDecimal taxAmount) {
        if (gstin == null || gstin.isBlank()) {
            return new VendorMatchResult(null, false, "NORMAL", 0, null);
        }
        BigDecimal amount = invoiceTotal == null ? BigDecimal.ZERO : invoiceTotal;
        BigDecimal tax    = taxAmount    == null ? BigDecimal.ZERO : taxAmount;
        return repository.findByGstin(gstin)
            .map(v -> updateStats(v, amount, tax))
            .orElseGet(() -> new VendorMatchResult(createNew(gstin, supplierName, amount, tax),
                false, "NORMAL", 0, computeGstRate(tax, amount)));
    }

    private VendorMatchResult updateStats(Vendor v, BigDecimal amount, BigDecimal tax) {
        boolean anomaly = false;

        // Anomaly: invoice amount > 3× historical average
        if (v.getTotalInvoiceCount() > 0 && v.getTotalInvoiceValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avg = v.getTotalInvoiceValue().divide(
                BigDecimal.valueOf(v.getTotalInvoiceCount()), 2, RoundingMode.HALF_UP);
            anomaly = amount.compareTo(avg.multiply(new BigDecimal("3.0"))) > 0;
        }

        v.setTotalInvoiceCount(v.getTotalInvoiceCount() + 1);
        v.setTotalInvoiceValue(v.getTotalInvoiceValue().add(amount));
        v.setLastSeenAt(Instant.now());

        // Update rolling typical GST rate (exponential moving average, α=0.3)
        BigDecimal gstRate = computeGstRate(tax, amount);
        if (gstRate != null) {
            if (v.getTypicalGstRate() == null) {
                v.setTypicalGstRate(gstRate);
            } else {
                BigDecimal alpha = new BigDecimal("0.3");
                BigDecimal newRate = gstRate.multiply(alpha)
                    .add(v.getTypicalGstRate().multiply(BigDecimal.ONE.subtract(alpha)))
                    .setScale(4, RoundingMode.HALF_UP);
                v.setTypicalGstRate(newRate);
            }
        }

        if (anomaly) {
            v.setAnomalyCount(v.getAnomalyCount() + 1);
            v.setLastAnomalyAt(Instant.now());
        }
        v.setRiskTier(deriveRiskTier(v.getAnomalyCount()));

        Vendor saved = repository.save(v);
        return new VendorMatchResult(saved, anomaly, saved.getRiskTier(),
            saved.getAnomalyCount(), saved.getTypicalGstRate());
    }

    private Vendor createNew(String gstin, String name, BigDecimal amount, BigDecimal tax) {
        Vendor v = new Vendor();
        v.setId(UUID.randomUUID());
        v.setGstin(gstin.trim().toUpperCase());
        v.setNormalizedName(normalizeName(name));
        v.setTotalInvoiceCount(1);
        v.setTotalInvoiceValue(amount);
        v.setTypicalGstRate(computeGstRate(tax, amount));
        v.setAnomalyCount(0);
        v.setRiskTier("NORMAL");
        Instant now = Instant.now();
        v.setFirstSeenAt(now);
        v.setLastSeenAt(now);
        return repository.save(v);
    }

    private BigDecimal computeGstRate(BigDecimal tax, BigDecimal amount) {
        if (tax == null || amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return null;
        try {
            return tax.divide(amount, 6, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100))
                      .setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private String deriveRiskTier(int anomalyCount) {
        if (anomalyCount == 0) return "LOW";
        if (anomalyCount <= 2) return "NORMAL";
        if (anomalyCount <= 5) return "ELEVATED";
        return "HIGH";
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) return "UNKNOWN";
        String n = SUFFIX_PATTERN.matcher(name.trim().toUpperCase()).replaceAll("").trim();
        return n.isBlank() ? name.trim().toUpperCase() : n;
    }
}
