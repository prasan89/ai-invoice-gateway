package com.aiinvoice.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "usage_records")
@Getter @Setter @NoArgsConstructor
public class UsageRecord {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(name = "invoice_count") private int invoiceCount = 0;
    @Column(name = "api_calls") private long apiCalls = 0;
    @Column(name = "ai_extractions") private int aiExtractions = 0;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
