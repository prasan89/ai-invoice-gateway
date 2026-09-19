package com.aiinvoice.invoice.dto;

import java.time.Instant;
import java.util.UUID;

public record InvoiceEventDto(
    UUID id,
    String eventType,
    String message,
    Instant createdAt
) {}
