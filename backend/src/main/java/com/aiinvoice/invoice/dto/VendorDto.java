package com.aiinvoice.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VendorDto(
    UUID id,
    String gstin,
    String normalizedName,
    int totalInvoiceCount,
    BigDecimal totalInvoiceValue,
    Instant firstSeenAt,
    Instant lastSeenAt
) {}
