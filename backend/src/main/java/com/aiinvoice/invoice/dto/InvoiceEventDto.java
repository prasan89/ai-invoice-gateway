package com.aiinvoice.invoice.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record InvoiceEventDto(
    UUID id,
    String eventType,
    String message,
    Instant createdAt,
    String actor,
    Map<String, Object> metadata
) {}
