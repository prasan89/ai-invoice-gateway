package com.aiinvoice.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "subscription_plans")
@Getter @Setter @NoArgsConstructor
public class SubscriptionPlan {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @Column(name = "display_name", nullable = false) private String displayName;
    @Column(name = "monthly_price_paise", nullable = false) private long monthlyPricePaise;
    @Column(name = "invoice_limit", nullable = false) private int invoiceLimit;
    @Column(name = "api_key_limit", nullable = false) private int apiKeyLimit;
    @Column(name = "user_limit", nullable = false) private int userLimit;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String features;
    private boolean active = true;
    @Column(name = "created_at") private Instant createdAt;
}
