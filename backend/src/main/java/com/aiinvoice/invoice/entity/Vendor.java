package com.aiinvoice.invoice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendors")
@Getter @Setter @NoArgsConstructor
public class Vendor {
    @Id private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String gstin;

    @Column(name = "normalized_name", nullable = false, length = 250)
    private String normalizedName;

    @Column(name = "total_invoice_count", nullable = false)
    private int totalInvoiceCount;

    @Column(name = "total_invoice_value", nullable = false)
    private BigDecimal totalInvoiceValue;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
}
