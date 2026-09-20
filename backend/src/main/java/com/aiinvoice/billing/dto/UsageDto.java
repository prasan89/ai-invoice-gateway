package com.aiinvoice.billing.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UsageDto(
    UUID id,
    LocalDate periodStart,
    LocalDate periodEnd,
    int invoiceCount,
    long apiCalls,
    int aiExtractions
) {}
