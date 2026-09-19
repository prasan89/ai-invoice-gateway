package com.aiinvoice.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "auth_events")
@Getter @Setter @NoArgsConstructor
public class AuthEvent {
    @Id private UUID id;
    @Column(name = "organization_id") private UUID organizationId;
    @Column(name = "user_id") private UUID userId;
    @Column(name = "event_type", nullable = false, length = 40) private String eventType;
    @Column(name = "actor_email", length = 320) private String actorEmail;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", columnDefinition = "TEXT") private String userAgent;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
