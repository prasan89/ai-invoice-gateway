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
 * Phase 5.2 — Advanced duplicate detection.
 *
 * Score bands:
 *   100  CONFIRMED  — exact document hash
 *    95  CONFIRMED  — same invoice# + same supplier GSTIN (invoice identity)
 *    85  CONFIRMED  — same invoice# + same supplier GSTIN + amount changed (amount-modified duplicate)
 *    80  CONFIRMED  — same invoice# + same supplier GSTIN + date changed (date-modified duplicate)
 *    75  POTENTIAL  — same invoice# + different supplier GSTIN (number collision / supplier switch)
 *    70  POTENTIAL  — same supplier GSTIN + same amount (±1%) + same date (same-day same-value)
 *    65  POTENTIAL  — same supplier GSTIN + same amount (±0.01) (possible re-upload)
 *    60  POTENTIAL  — same supplier GSTIN + amount within 5% + same date (near-identical)
 *     0  NONE       — no meaningful match
 *
 * Each result carries a human-readable `reason` string explaining the match type.
 */
@Service
public class InvoiceDuplicateService {

    private final InvoiceRepository repository;

    public InvoiceDuplicateService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public record DuplicateCheckResult(int score, UUID duplicateInvoiceId, String reason) {}

    public String computeHash(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(fileBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public DuplicateCheckResult check(Invoice invoice) {
        // Exact document hash → confirmed
        if (invoice.getDocumentHash() != null) {
            var exact = repository.findFirstByDocumentHashAndIdNot(
                invoice.getDocumentHash(), invoice.getId());
            if (exact.isPresent()) {
                return new DuplicateCheckResult(100, exact.get().getId(),
                    "Exact document hash match — identical file re-uploaded");
            }
        }

        if (invoice.getOrganizationId() == null) {
            return new DuplicateCheckResult(0, null, null);
        }

        List<Invoice> candidates = repository.findDuplicateCandidates(
            invoice.getOrganizationId(),
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getSupplierGstin());

        int bestScore = 0;
        UUID bestId = null;
        String bestReason = null;

        for (Invoice candidate : candidates) {
            ScoredMatch match = score(invoice, candidate);
            if (match.score() > bestScore) {
                bestScore = match.score();
                bestId = candidate.getId();
                bestReason = match.reason();
            }
        }

        return new DuplicateCheckResult(bestScore, bestId, bestReason);
    }

    private record ScoredMatch(int score, String reason) {}

    private ScoredMatch score(Invoice a, Invoice b) {
        boolean sameInvNum        = a.getInvoiceNumber() != null && a.getInvoiceNumber().equals(b.getInvoiceNumber());
        boolean sameSupplierGstin = a.getSupplierGstin() != null && a.getSupplierGstin().equals(b.getSupplierGstin());
        boolean sameAmount        = withinAbsolute(a.getTotalAmount(), b.getTotalAmount(), new BigDecimal("0.01"));
        boolean nearAmount        = withinPercent(a.getTotalAmount(), b.getTotalAmount(), 1.0);
        boolean veryNearAmount    = withinPercent(a.getTotalAmount(), b.getTotalAmount(), 5.0);
        boolean sameDate          = a.getInvoiceDate() != null && a.getInvoiceDate().equals(b.getInvoiceDate());

        // Same invoice identity (exact)
        if (sameInvNum && sameSupplierGstin && sameAmount && sameDate)
            return new ScoredMatch(95, "Same invoice number, supplier GSTIN, amount and date — confirmed identity duplicate");

        // Amount-modified duplicate (same invoice# and supplier, different amount)
        if (sameInvNum && sameSupplierGstin && !sameAmount)
            return new ScoredMatch(85, "Same invoice number and supplier GSTIN but different amount — possible amount-modified duplicate");

        // Date-modified duplicate (same invoice# and supplier, different date)
        if (sameInvNum && sameSupplierGstin)
            return new ScoredMatch(80, "Same invoice number and supplier GSTIN but different date — possible date-modified duplicate");

        // Invoice number collision with different supplier
        if (sameInvNum && !sameSupplierGstin)
            return new ScoredMatch(75, "Same invoice number but different supplier GSTIN — possible invoice number collision or supplier switch");

        // Same-day same-value from same supplier (near-1%)
        if (sameSupplierGstin && nearAmount && sameDate)
            return new ScoredMatch(70, "Same supplier, same date, and amount within 1% — likely OCR variation or PDF re-upload");

        // Same supplier exact amount (no date match)
        if (sameSupplierGstin && sameAmount)
            return new ScoredMatch(65, "Same supplier GSTIN and identical amount — possible re-upload of the same invoice");

        // Near-identical amount within 5% from same supplier on same date
        if (sameSupplierGstin && veryNearAmount && sameDate)
            return new ScoredMatch(60, "Same supplier, same date, amount within 5% — near-identical invoice, requires review");

        return new ScoredMatch(0, null);
    }

    private boolean withinAbsolute(BigDecimal a, BigDecimal b, BigDecimal tolerance) {
        if (a == null || b == null) return false;
        return a.subtract(b).abs().compareTo(tolerance) <= 0;
    }

    private boolean withinPercent(BigDecimal a, BigDecimal b, double pct) {
        if (a == null || b == null) return false;
        if (a.compareTo(BigDecimal.ZERO) == 0 && b.compareTo(BigDecimal.ZERO) == 0) return true;
        if (a.compareTo(BigDecimal.ZERO) == 0) return false;
        double diff = a.subtract(b).abs().doubleValue() / a.doubleValue() * 100.0;
        return diff <= pct;
    }
}
