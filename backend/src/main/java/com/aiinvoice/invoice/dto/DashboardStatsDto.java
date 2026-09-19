package com.aiinvoice.invoice.dto;

import java.math.BigDecimal;

public record DashboardStatsDto(
    long totalCount,
    BigDecimal totalValue,
    long reviewRequiredCount,
    long approvedCount,
    long failedCount,
    long rejectedCount,
    long autoApprovedCount,
    long potentialDuplicatesCount,
    BigDecimal averageConfidence
) {}
