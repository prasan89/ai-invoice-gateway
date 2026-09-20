package com.aiinvoice.enterprise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "approval_requests")
@Getter @Setter @NoArgsConstructor
public class ApprovalRequest {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "invoice_id", nullable = false) private UUID invoiceId;
    @Column(name = "chain_id", nullable = false) private UUID chainId;
    @Column(name = "current_step", nullable = false) private int currentStep = 0;
    @Column(nullable = false) private String status = "PENDING";
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_state", columnDefinition = "jsonb") private String stepsState;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
