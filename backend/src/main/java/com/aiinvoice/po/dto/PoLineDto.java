package com.aiinvoice.po.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PoLineDto(
    UUID id,
    int lineNumber,
    String description,
    String hsnSac,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal taxRate,
    BigDecimal lineTotal
) {}
