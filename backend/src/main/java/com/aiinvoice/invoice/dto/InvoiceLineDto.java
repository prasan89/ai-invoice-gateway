package com.aiinvoice.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineDto(
  UUID id,
  @NotBlank String description,
  String hsnSac,
  @DecimalMin("0") BigDecimal quantity,
  @DecimalMin("0") BigDecimal unitPrice,
  @DecimalMin("0") BigDecimal discount,
  BigDecimal taxableValue,
  BigDecimal taxRate,
  BigDecimal taxAmount,
  BigDecimal cgstRate,
  BigDecimal cgstAmount,
  BigDecimal sgstRate,
  BigDecimal sgstAmount,
  BigDecimal igstRate,
  BigDecimal igstAmount,
  BigDecimal cessRate,
  BigDecimal cessAmount,
  BigDecimal lineTotal
) {}
