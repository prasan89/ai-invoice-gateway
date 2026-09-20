package com.aiinvoice.copilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "copilot_sessions")
@Getter @Setter @NoArgsConstructor
public class CopilotSession {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    private String title;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
