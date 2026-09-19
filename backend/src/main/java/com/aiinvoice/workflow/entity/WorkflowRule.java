package com.aiinvoice.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "workflow_rules")
@Getter @Setter @NoArgsConstructor
public class WorkflowRule {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private int priority = 100;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String conditions;
    @Column(nullable = false, length = 40) private String action;
    @Column(name = "required_role", length = 40) private String requiredRole;
    @Column(name = "next_state", nullable = false, length = 40) private String nextState;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
