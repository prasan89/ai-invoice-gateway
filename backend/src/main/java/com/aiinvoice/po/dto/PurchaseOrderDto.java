package com.aiinvoice.po.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderDto(
    UUID id,
    String poNumber,
    String supplierGstin,
    String supplierName,
    LocalDate poDate,
    String currency,
    BigDecimal totalAmount,
    String status,
    String notes,
    List<PoLineDto> lines
) {}
