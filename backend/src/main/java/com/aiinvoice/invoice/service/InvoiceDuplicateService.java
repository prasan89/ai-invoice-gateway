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
        // Exact hash match → score 100
        if (invoice.getDocumentHash() != null) {
            var exact = repository.findFirstByDocumentHashAndIdNot(
                invoice.getDocumentHash(), invoice.getId());
            if (exact.isPresent()) {
                return new DuplicateCheckResult(100, exact.get().getId());
            }
        }

        // Business duplicate scoring
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
            int score = 0;
            if (invoice.getInvoiceNumber() != null
                && invoice.getInvoiceNumber().equals(candidate.getInvoiceNumber())) {
                score += 25;
            }
            if (invoice.getSupplierGstin() != null
                && invoice.getSupplierGstin().equals(candidate.getSupplierGstin())) {
                score += 20;
            }
            if (invoice.getTotalAmount() != null && candidate.getTotalAmount() != null
                && invoice.getTotalAmount().subtract(candidate.getTotalAmount()).abs()
                    .compareTo(new BigDecimal("0.01")) <= 0) {
                score += 10;
            }
            if (invoice.getInvoiceDate() != null
                && invoice.getInvoiceDate().equals(candidate.getInvoiceDate())) {
                score += 5;
            }
            if (score > bestScore) {
                bestScore = score;
                bestId = candidate.getId();
            }
        }

        return new DuplicateCheckResult(bestScore, bestId);
    }
}
