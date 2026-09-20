package com.aiinvoice.billing.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
    UUID id,
    String planName,
    String planDisplayName,
    long monthlyPricePaise,
    int invoiceLimit,
    int apiKeyLimit,
    int userLimit,
    List<String> features,
    String status,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Instant trialEnd,
    int invoiceCountCurrent,
    int invoiceCountRemaining
) {}
