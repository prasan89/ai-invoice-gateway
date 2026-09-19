package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Score bands:
 *   >= 90  CONFIRMED  — exact hash, or same invoice# + same supplier GSTIN
 *   60–89  POTENTIAL  — strong business similarity, not conclusive
 *   < 60   NONE       — no meaningful match
 *
 * Signal weights:
 *   Exact document hash                        → 100 (short-circuit)
 *   invoice# + supplierGstin                   →  90 (same invoice identity)
 *   invoice# alone                             →  50
 *   supplierGstin + amount (≤0.01) + date      →  35
 *   supplierGstin + amount (≤0.01)             →  25
 *   Any single signal alone (gstin/amount/date)→   0 (too weak — a supplier
 *                                                      can legitimately issue
 *                                                      many invoices for the same
 *                                                      amount to the same customer)
 */
@Service
public class InvoiceDuplicateService {

    private final InvoiceRepository repository;

    public InvoiceDuplicateService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public record DuplicateCheckResult(int score, UUID duplicateInvoiceId) {}

    public String computeHash(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(fileBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public DuplicateCheckResult check(Invoice invoice) {
        // Exact document hash → confirmed duplicate
        if (invoice.getDocumentHash() != null) {
            var exact = repository.findFirstByDocumentHashAndIdNot(
                invoice.getDocumentHash(), invoice.getId());
            if (exact.isPresent()) {
                return new DuplicateCheckResult(100, exact.get().getId());
            }
        }

        if (invoice.getOrganizationId() == null) {
            return new DuplicateCheckResult(0, null);
        }

        List<Invoice> candidates = repository.findDuplicateCandidates(
            invoice.getOrganizationId(),
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getSupplierGstin());

        int bestScore = 0;
        UUID bestId = null;

        for (Invoice candidate : candidates) {
            int score = businessScore(invoice, candidate);
            if (score > bestScore) {
                bestScore = score;
                bestId = candidate.getId();
            }
        }

        return new DuplicateCheckResult(bestScore, bestId);
    }

    private int businessScore(Invoice a, Invoice b) {
        boolean sameInvNum = a.getInvoiceNumber() != null
            && a.getInvoiceNumber().equals(b.getInvoiceNumber());
        boolean sameSupplierGstin = a.getSupplierGstin() != null
            && a.getSupplierGstin().equals(b.getSupplierGstin());
        boolean sameAmount = a.getTotalAmount() != null && b.getTotalAmount() != null
            && a.getTotalAmount().subtract(b.getTotalAmount()).abs()
                .compareTo(new BigDecimal("0.01")) <= 0;
        boolean sameDate = a.getInvoiceDate() != null
            && a.getInvoiceDate().equals(b.getInvoiceDate());

        // Same invoice identity — very strong
        if (sameInvNum && sameSupplierGstin) return 90;

        // Invoice number alone is a strong signal
        if (sameInvNum) return 50;

        // Business signals are only meaningful in combination
        if (sameSupplierGstin && sameAmount && sameDate) return 35;
        if (sameSupplierGstin && sameAmount) return 25;

        // No single weak signal (GSTIN/amount/date alone) ever triggers a flag
        return 0;
    }
}
