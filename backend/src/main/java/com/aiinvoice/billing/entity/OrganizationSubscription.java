package com.aiinvoice.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "organization_subscriptions")
@Getter @Setter @NoArgsConstructor
public class OrganizationSubscription {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false, unique = true) private UUID organizationId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false) private SubscriptionPlan plan;
    @Column(nullable = false) private String status = "TRIALING";
    @Column(name = "razorpay_sub_id") private String razorpaySubId;
    @Column(name = "razorpay_customer_id") private String razorpayCustomerId;
    @Column(name = "current_period_start") private Instant currentPeriodStart;
    @Column(name = "current_period_end") private Instant currentPeriodEnd;
    @Column(name = "trial_end") private Instant trialEnd;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "invoice_count_current") private int invoiceCountCurrent = 0;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
