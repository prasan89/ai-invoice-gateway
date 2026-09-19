package com.aiinvoice.po.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePoRequest(
    String poNumber,
    String supplierGstin,
    String supplierName,
    LocalDate poDate,
    String currency,
    String notes,
    List<PoLineRequest> lines
) {
    public record PoLineRequest(
        int lineNumber,
        String description,
        String hsnSac,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate
    ) {}
}
