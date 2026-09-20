package com.aiinvoice.billing.dto;

import java.util.List;
import java.util.UUID;

public record PlanDto(
    UUID id,
    String name,
    String displayName,
    long monthlyPricePaise,
    int invoiceLimit,
    int apiKeyLimit,
    int userLimit,
    List<String> features
) {}
