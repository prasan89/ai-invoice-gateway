package com.aiinvoice.po.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateGrnRequest(
    UUID poId,
    String grnNumber,
    LocalDate receiptDate,
    String notes,
    List<GrnLineRequest> lines
) {
    public record GrnLineRequest(
        UUID poLineId,
        int lineNumber,
        String description,
        String hsnSac,
        BigDecimal quantityReceived,
        BigDecimal unitPrice
    ) {}
}
