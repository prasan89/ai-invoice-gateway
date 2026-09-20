package com.aiinvoice.anomaly.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnomalyDto(
    UUID id,
    UUID invoiceId,
    int riskScore,
    String riskLevel,
    List<String> anomalyTypes,
    List<String> reasons,
    Instant detectedAt,
    String reviewOutcome
) {}
