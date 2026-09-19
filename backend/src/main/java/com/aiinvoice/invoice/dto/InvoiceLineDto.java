package com.aiinvoice.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineDto(
  UUID id,
  @NotBlank String description,
  @DecimalMin("0") BigDecimal quantity,
  @DecimalMin("0") BigDecimal unitPrice,
  @DecimalMin("0") BigDecimal discount,
  @DecimalMin("0") BigDecimal taxRate,
  BigDecimal taxAmount,
  BigDecimal lineTotal
) {}
