package com.aiinvoice.ai;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "demo", matchIfMissing = true)
public class DemoInvoiceExtractor implements InvoiceExtractor {

  @Override
  public InvoiceExtractionResult extract(MultipartFile document) {
    BigDecimal subtotal  = new BigDecimal("100000.00");
    BigDecimal cgst      = new BigDecimal("9000.00");
    BigDecimal sgst      = new BigDecimal("9000.00");
    BigDecimal tax       = new BigDecimal("18000.00");
    BigDecimal total     = new BigDecimal("118000.00");

    InvoiceLineDto line = new InvoiceLineDto(
        null, "Software Services", "998314",
        BigDecimal.ONE, subtotal, BigDecimal.ZERO, subtotal,
        new BigDecimal("18"), tax,
        new BigDecimal("9"), cgst,
        new BigDecimal("9"), sgst,
        null, null, null, null,
        total);

    Map<String, BigDecimal> confidence = new LinkedHashMap<>();
    confidence.put("invoiceNumber",   new BigDecimal("96.5"));
    confidence.put("invoiceDate",     new BigDecimal("96.5"));
    confidence.put("currency",        new BigDecimal("99.0"));
    confidence.put("supplierName",    new BigDecimal("97.0"));
    confidence.put("supplierGstin",   new BigDecimal("94.0"));
    confidence.put("customerName",    new BigDecimal("97.0"));
    confidence.put("customerGstin",   new BigDecimal("94.0"));
    confidence.put("taxableSubtotal", new BigDecimal("98.0"));
    confidence.put("cgstAmount",      new BigDecimal("98.0"));
    confidence.put("sgstAmount",      new BigDecimal("98.0"));
    confidence.put("igstAmount",      new BigDecimal("99.0"));
    confidence.put("cessAmount",      new BigDecimal("99.0"));
    confidence.put("taxAmount",       new BigDecimal("98.0"));
    confidence.put("totalAmount",     new BigDecimal("99.0"));
    confidence.put("lines",           new BigDecimal("95.0"));

    InvoiceDto invoice = new InvoiceDto(
        null, "DEMO-INV-1001", LocalDate.now(), "INR",
        "Demo Supplier Pvt Ltd",  "29AAAPL2345A1Z8",
        "Demo Customer Pvt Ltd",  "33BBBPL3456B1Z7",
        subtotal, tax, cgst, sgst, null, null, total,
        new BigDecimal("96.50"), InvoiceStatus.EXTRACTED, null,
        List.of(line), confidence, null, null, null,
        null, null, null, null, null, null, null, null, null, false);

    return new InvoiceExtractionResult(invoice, 96.5);
  }
}
