package com.aiinvoice.enterprise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "approval_chains")
@Getter @Setter @NoArgsConstructor
public class ApprovalChain {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(nullable = false) private String name;
    private String description;
    @Column(name = "min_amount") private BigDecimal minAmount;
    @Column(name = "max_amount") private BigDecimal maxAmount;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String steps;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
