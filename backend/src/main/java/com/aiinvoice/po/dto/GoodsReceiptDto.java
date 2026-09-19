package com.aiinvoice.po.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptDto(
    UUID id,
    UUID poId,
    String grnNumber,
    LocalDate receiptDate,
    String status,
    String notes,
    List<GrnLineDto> lines
) {}
