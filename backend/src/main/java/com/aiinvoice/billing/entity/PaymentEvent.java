package com.aiinvoice.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "payment_events")
@Getter @Setter @NoArgsConstructor
public class PaymentEvent {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "subscription_id") private UUID subscriptionId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "razorpay_event_id", unique = true) private String razorpayEventId;
    @Column(name = "amount_paise") private Long amountPaise;
    private String currency = "INR";
    private String status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String payload;
    @Column(name = "created_at") private Instant createdAt;
}
