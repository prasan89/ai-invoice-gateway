package com.aiinvoice.invoice.dto;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceReviewRequest(
    String invoiceNumber,
    LocalDate invoiceDate,
    String currency,
    String supplierName,
    String supplierGstin,
    String customerName,
    String customerGstin,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    @Valid List<InvoiceLineDto> lines
) {}
