package com.aiinvoice.anomaly.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "invoice_anomalies")
@Getter @Setter @NoArgsConstructor
public class InvoiceAnomaly {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "invoice_id", nullable = false) private UUID invoiceId;
    @Column(name = "risk_score", nullable = false) private int riskScore;
    @Column(name = "risk_level", nullable = false) private String riskLevel;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anomaly_types", columnDefinition = "jsonb") private String anomalyTypes;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String reasons;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vendor_baseline", columnDefinition = "jsonb") private String vendorBaseline;
    @Column(name = "detected_at") private Instant detectedAt;
    @Column(name = "reviewed_by") private UUID reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "review_outcome") private String reviewOutcome;
}
