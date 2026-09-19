package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Vendor;
import com.aiinvoice.invoice.repository.VendorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class VendorService {

    private static final Pattern SUFFIX_PATTERN = Pattern.compile(
        "\\b(PVT|PRIVATE|LTD|LIMITED|LLP|INC|CORP|CO)\\b\\.?\\s*$",
        Pattern.CASE_INSENSITIVE);

    public record VendorMatchResult(Vendor vendor, boolean anomalyFlag) {}

    private final VendorRepository repository;

    public VendorService(VendorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VendorMatchResult matchOrCreate(String gstin, String supplierName, BigDecimal invoiceTotal) {
        if (gstin == null || gstin.isBlank()) return new VendorMatchResult(null, false);
        BigDecimal amount = invoiceTotal == null ? BigDecimal.ZERO : invoiceTotal;
        return repository.findByGstin(gstin)
            .map(v -> updateStats(v, amount))
            .orElseGet(() -> new VendorMatchResult(createNew(gstin, supplierName, amount), false));
    }

    private VendorMatchResult updateStats(Vendor v, BigDecimal amount) {
        boolean anomaly = false;
        if (v.getTotalInvoiceCount() > 0 && v.getTotalInvoiceValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avg = v.getTotalInvoiceValue().divide(
                BigDecimal.valueOf(v.getTotalInvoiceCount()), 2, java.math.RoundingMode.HALF_UP);
            anomaly = amount.compareTo(avg.multiply(new BigDecimal("3.0"))) > 0;
        }
        v.setTotalInvoiceCount(v.getTotalInvoiceCount() + 1);
        v.setTotalInvoiceValue(v.getTotalInvoiceValue().add(amount));
        v.setLastSeenAt(Instant.now());
        return new VendorMatchResult(repository.save(v), anomaly);
    }

    private Vendor createNew(String gstin, String name, BigDecimal amount) {
        Vendor v = new Vendor();
        v.setId(UUID.randomUUID());
        v.setGstin(gstin.trim().toUpperCase());
        v.setNormalizedName(normalizeName(name));
        v.setTotalInvoiceCount(1);
        v.setTotalInvoiceValue(amount);
        Instant now = Instant.now();
        v.setFirstSeenAt(now);
        v.setLastSeenAt(now);
        return repository.save(v);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) return "UNKNOWN";
        String n = SUFFIX_PATTERN.matcher(name.trim().toUpperCase()).replaceAll("").trim();
        return n.isBlank() ? name.trim().toUpperCase() : n;
    }
}
