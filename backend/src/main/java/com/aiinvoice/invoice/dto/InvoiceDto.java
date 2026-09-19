package com.aiinvoice.invoice.dto;

import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.domain.GstinValidationStatus;
import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.domain.ValidationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InvoiceDto(
  UUID id,
  @NotBlank String invoiceNumber,
  LocalDate invoiceDate,
  String currency,
  String supplierName,
  String supplierGstin,
  String customerName,
  String customerGstin,
  BigDecimal subtotal,
  BigDecimal taxAmount,
  BigDecimal cgstAmount,
  BigDecimal sgstAmount,
  BigDecimal igstAmount,
  BigDecimal cessAmount,
  BigDecimal totalAmount,
  BigDecimal extractionConfidence,
  InvoiceStatus status,
  String validationMessage,
  @Valid List<InvoiceLineDto> lines,
  Map<String, BigDecimal> fieldConfidence,
  String sourceFileName,
  String sourceContentType,
  String documentUrl,
  List<ValidationResult> validationResults,
  GstinValidationStatus supplierGstinStatus,
  GstinValidationStatus customerGstinStatus,
  ArithmeticStatus arithmeticStatus,
  Integer duplicateScore,
  UUID duplicateInvoiceId,
  UUID vendorId,
  String vendorNormalizedName
) {}
