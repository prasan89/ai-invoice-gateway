package com.aiinvoice.webhook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "webhook_deliveries")
@Getter @Setter @NoArgsConstructor
public class WebhookDelivery {
    @Id private UUID id;
    @Column(name = "subscription_id", nullable = false) private UUID subscriptionId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(nullable = false) private int attempts = 0;
    @Column(name = "last_attempt_at") private Instant lastAttemptAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
