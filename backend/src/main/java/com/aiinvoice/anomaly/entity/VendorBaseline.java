package com.aiinvoice.anomaly.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "vendor_baselines")
@Getter @Setter @NoArgsConstructor
public class VendorBaseline {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "supplier_gstin", nullable = false) private String supplierGstin;
    @Column(name = "supplier_name") private String supplierName;
    @Column(name = "invoice_count") private int invoiceCount = 0;
    @Column(name = "avg_amount") private BigDecimal avgAmount;
    @Column(name = "stddev_amount") private BigDecimal stddevAmount;
    @Column(name = "min_amount") private BigDecimal minAmount;
    @Column(name = "max_amount") private BigDecimal maxAmount;
    @Column(name = "typical_gst_rate") private BigDecimal typicalGstRate;
    @Column(name = "first_seen") private Instant firstSeen;
    @Column(name = "last_seen") private Instant lastSeen;
    @Column(name = "updated_at") private Instant updatedAt;
}
