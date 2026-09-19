package com.aiinvoice.po.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import com.aiinvoice.po.entity.*;
import com.aiinvoice.po.repository.GoodsReceiptRepository;
import com.aiinvoice.po.repository.InvoicePoMatchRepository;
import com.aiinvoice.po.repository.PurchaseOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PoMatchingService {

    private static final BigDecimal TOLERANCE_PCT = new BigDecimal("0.02"); // 2%

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository poRepository;
    private final GoodsReceiptRepository grnRepository;
    private final InvoicePoMatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    public record MatchResult(
        String matchType,
        String matchStatus,
        List<Discrepancy> discrepancies
    ) {}

    public record Discrepancy(
        String field,
        String expected,
        String actual,
        String severity
    ) {}

    /**
     * Attempt 2-way match: Invoice ↔ PO
     */
    @Transactional
    public MatchResult matchTwoWay(UUID invoiceId, UUID poId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
        PurchaseOrder po = poRepository.findByIdWithLines(poId)
            .orElseThrow(() -> new NoSuchElementException("PO not found: " + poId));

        List<Discrepancy> discrepancies = compareTwoWay(invoice, po);
        String matchStatus = determineMatchStatus(discrepancies, invoice.getTotalAmount(), po.getTotalAmount());

        InvoicePoMatch match = buildMatch(invoiceId, poId, null, "TWO_WAY", matchStatus, discrepancies);
        matchRepository.save(match);

        invoice.setPoMatchStatus(matchStatus);
        invoice.setMatchedPoId(poId);
        invoiceRepository.save(invoice);

        return new MatchResult("TWO_WAY", matchStatus, discrepancies);
    }

    /**
     * Attempt 3-way match: Invoice ↔ PO ↔ GRN
     */
    @Transactional
    public MatchResult matchThreeWay(UUID invoiceId, UUID poId, UUID grnId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
        PurchaseOrder po = poRepository.findByIdWithLines(poId)
            .orElseThrow(() -> new NoSuchElementException("PO not found: " + poId));
        GoodsReceipt grn = grnRepository.findByIdWithLines(grnId)
            .orElseThrow(() -> new NoSuchElementException("GRN not found: " + grnId));

        List<Discrepancy> discrepancies = new ArrayList<>();
        discrepancies.addAll(compareTwoWay(invoice, po));
        discrepancies.addAll(compareGrnToPo(grn, po));
        discrepancies.addAll(compareInvoiceToGrn(invoice, grn, po));

        String matchStatus = determineMatchStatus(discrepancies, invoice.getTotalAmount(), po.getTotalAmount());

        InvoicePoMatch match = buildMatch(invoiceId, poId, grnId, "THREE_WAY", matchStatus, discrepancies);
        matchRepository.save(match);

        invoice.setPoMatchStatus(matchStatus);
        invoice.setMatchedPoId(poId);
        invoiceRepository.save(invoice);

        return new MatchResult("THREE_WAY", matchStatus, discrepancies);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<Discrepancy> compareTwoWay(Invoice invoice, PurchaseOrder po) {
        List<Discrepancy> out = new ArrayList<>();

        // Supplier GSTIN must match
        if (invoice.getSupplierGstin() != null && po.getSupplierGstin() != null
                && !invoice.getSupplierGstin().equalsIgnoreCase(po.getSupplierGstin())) {
            out.add(new Discrepancy("supplierGstin", po.getSupplierGstin(), invoice.getSupplierGstin(), "HIGH"));
        }

        // Total amount within tolerance
        if (invoice.getTotalAmount() != null && po.getTotalAmount() != null) {
            BigDecimal diff = invoice.getTotalAmount().subtract(po.getTotalAmount()).abs();
            BigDecimal threshold = po.getTotalAmount().multiply(TOLERANCE_PCT).abs();
            if (diff.compareTo(threshold) > 0) {
                String severity = diff.compareTo(po.getTotalAmount().multiply(new BigDecimal("0.10"))) > 0
                    ? "HIGH" : "MEDIUM";
                out.add(new Discrepancy(
                    "totalAmount",
                    po.getTotalAmount().toPlainString(),
                    invoice.getTotalAmount().toPlainString(),
                    severity
                ));
            }
        }

        // Tax amount check
        if (invoice.getTaxAmount() != null && po.getTotalAmount() != null) {
            BigDecimal poTax = computeExpectedTaxFromLines(po.getLines());
            if (poTax.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxDiff = invoice.getTaxAmount().subtract(poTax).abs();
                BigDecimal taxThreshold = poTax.multiply(TOLERANCE_PCT);
                if (taxDiff.compareTo(taxThreshold) > 0) {
                    out.add(new Discrepancy("taxAmount", poTax.toPlainString(),
                        invoice.getTaxAmount().toPlainString(), "MEDIUM"));
                }
            }
        }

        return out;
    }

    private List<Discrepancy> compareGrnToPo(GoodsReceipt grn, PurchaseOrder po) {
        List<Discrepancy> out = new ArrayList<>();
        Map<UUID, PoLine> poLineMap = new HashMap<>();
        for (PoLine pl : po.getLines()) poLineMap.put(pl.getId(), pl);

        for (GrnLine gl : grn.getLines()) {
            if (gl.getPoLineId() == null) continue;
            PoLine poLine = poLineMap.get(gl.getPoLineId());
            if (poLine == null) {
                out.add(new Discrepancy("grnLine." + gl.getLineNumber(),
                    "linked PO line", "no matching PO line found", "HIGH"));
                continue;
            }
            if (gl.getQuantityReceived() != null && poLine.getQuantity() != null) {
                if (gl.getQuantityReceived().compareTo(poLine.getQuantity()) > 0) {
                    out.add(new Discrepancy(
                        "grnLine." + gl.getLineNumber() + ".quantityReceived",
                        poLine.getQuantity().toPlainString(),
                        gl.getQuantityReceived().toPlainString(),
                        "MEDIUM"
                    ));
                }
            }
        }
        return out;
    }

    private List<Discrepancy> compareInvoiceToGrn(Invoice invoice, GoodsReceipt grn, PurchaseOrder po) {
        List<Discrepancy> out = new ArrayList<>();
        // Invoice line count vs GRN line count
        int invoiceLines = invoice.getLines() != null ? invoice.getLines().size() : 0;
        int grnLines = grn.getLines() != null ? grn.getLines().size() : 0;
        if (invoiceLines != grnLines) {
            out.add(new Discrepancy("lineCount",
                String.valueOf(grnLines), String.valueOf(invoiceLines), "LOW"));
        }

        // Invoice billed amount should not exceed GRN-confirmed value
        if (invoice.getTotalAmount() != null && po.getTotalAmount() != null) {
            BigDecimal grnValue = computeGrnValue(grn, po);
            if (grnValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = grnValue.multiply(TOLERANCE_PCT.add(BigDecimal.ONE));
                if (invoice.getTotalAmount().compareTo(threshold) > 0) {
                    out.add(new Discrepancy("overbilling",
                        "≤" + grnValue.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        invoice.getTotalAmount().toPlainString(),
                        "HIGH"
                    ));
                }
            }
        }
        return out;
    }

    private BigDecimal computeExpectedTaxFromLines(List<PoLine> lines) {
        if (lines == null) return BigDecimal.ZERO;
        return lines.stream()
            .filter(l -> l.getTaxRate() != null && l.getUnitPrice() != null && l.getQuantity() != null)
            .map(l -> l.getUnitPrice().multiply(l.getQuantity()).multiply(l.getTaxRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeGrnValue(GoodsReceipt grn, PurchaseOrder po) {
        Map<UUID, PoLine> poLineMap = new HashMap<>();
        for (PoLine pl : po.getLines()) poLineMap.put(pl.getId(), pl);
        BigDecimal total = BigDecimal.ZERO;
        for (GrnLine gl : grn.getLines()) {
            if (gl.getPoLineId() == null || gl.getQuantityReceived() == null) continue;
            PoLine poLine = poLineMap.get(gl.getPoLineId());
            if (poLine == null || poLine.getUnitPrice() == null) continue;
            BigDecimal lineVal = poLine.getUnitPrice().multiply(gl.getQuantityReceived());
            if (poLine.getTaxRate() != null) {
                lineVal = lineVal.multiply(BigDecimal.ONE.add(
                    poLine.getTaxRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
            }
            total = total.add(lineVal);
        }
        return total;
    }

    private String determineMatchStatus(List<Discrepancy> discrepancies,
                                        BigDecimal invoiceTotal, BigDecimal poTotal) {
        if (discrepancies.isEmpty()) return "MATCHED";
        boolean hasHigh = discrepancies.stream().anyMatch(d -> "HIGH".equals(d.severity()));
        if (hasHigh) {
            // Over-billed vs under-billed
            if (invoiceTotal != null && poTotal != null) {
                return invoiceTotal.compareTo(poTotal) > 0 ? "OVER_BILLED" : "UNDER_BILLED";
            }
            return "UNMATCHED";
        }
        return "PARTIAL";
    }

    private InvoicePoMatch buildMatch(UUID invoiceId, UUID poId, UUID grnId,
                                      String matchType, String matchStatus,
                                      List<Discrepancy> discrepancies) {
        InvoicePoMatch m = new InvoicePoMatch();
        m.setId(UUID.randomUUID());
        m.setInvoiceId(invoiceId);
        m.setPoId(poId);
        m.setGrnId(grnId);
        m.setMatchType(matchType);
        m.setMatchStatus(matchStatus);
        m.setMatchedAt(Instant.now());
        try {
            m.setDiscrepancies(objectMapper.writeValueAsString(discrepancies));
        } catch (JsonProcessingException e) {
            m.setDiscrepancies("[]");
        }
        return m;
    }
}
