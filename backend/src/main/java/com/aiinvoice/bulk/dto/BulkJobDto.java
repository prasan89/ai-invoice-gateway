package com.aiinvoice.bulk.dto;

import java.time.Instant;
import java.util.UUID;

public record BulkJobDto(
    UUID id,
    String status,
    int totalCount,
    int processedCount,
    int failedCount,
    int progressPct,
    Instant createdAt
) {}
