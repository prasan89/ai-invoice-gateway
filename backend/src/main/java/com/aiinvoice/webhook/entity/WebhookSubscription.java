package com.aiinvoice.webhook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "webhook_subscriptions")
@Getter @Setter @NoArgsConstructor
public class WebhookSubscription {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(nullable = false, length = 2000) private String url;
    @Column(columnDefinition = "TEXT[]") private String[] events;
    @Column(nullable = false, length = 100) private String secret;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
