package com.aiinvoice.po.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GrnLineDto(
    UUID id,
    UUID poLineId,
    int lineNumber,
    String description,
    String hsnSac,
    BigDecimal quantityReceived,
    BigDecimal unitPrice
) {}
