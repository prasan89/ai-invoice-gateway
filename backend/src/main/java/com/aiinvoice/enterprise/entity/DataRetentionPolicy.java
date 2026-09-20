package com.aiinvoice.enterprise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "data_retention_policies")
@Getter @Setter @NoArgsConstructor
public class DataRetentionPolicy {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false, unique = true) private UUID organizationId;
    @Column(name = "invoice_retention_days") private int invoiceRetentionDays = 2555;
    @Column(name = "audit_retention_days") private int auditRetentionDays = 2555;
    @Column(name = "storage_retention_days") private int storageRetentionDays = 2555;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
