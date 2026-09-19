package com.aiinvoice.ai;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "demo")
public class DemoInvoiceExtractor implements InvoiceExtractor {
  @Override
  public InvoiceExtractionResult extract(org.springframework.web.multipart.MultipartFile document) {
    BigDecimal subtotal = new BigDecimal("100000.00");
    BigDecimal tax = new BigDecimal("18000.00");
    BigDecimal total = new BigDecimal("118000.00");
    InvoiceLineDto line = new InvoiceLineDto(null, "Software Services", BigDecimal.ONE,
      subtotal, BigDecimal.ZERO, new BigDecimal("18"), tax, total);

    Map<String, BigDecimal> confidence = new LinkedHashMap<>();
    confidence.put("invoiceNumber", new BigDecimal("96.5"));
    confidence.put("invoiceDate", new BigDecimal("96.5"));
    confidence.put("currency", new BigDecimal("99"));
    confidence.put("supplierName", new BigDecimal("97"));
    confidence.put("supplierGstin", new BigDecimal("94"));
    confidence.put("customerName", new BigDecimal("97"));
    confidence.put("customerGstin", new BigDecimal("94"));
    confidence.put("subtotal", new BigDecimal("98"));
    confidence.put("taxAmount", new BigDecimal("98"));
    confidence.put("totalAmount", new BigDecimal("99"));
    confidence.put("lines", new BigDecimal("95"));

    InvoiceDto invoice = new InvoiceDto(null, "DEMO-INV-1001", LocalDate.now(), "INR",
      "Demo Supplier Pvt Ltd", "29AAAAA0000A1Z5",
      "Demo Customer Pvt Ltd", "33BBBBB0000B1Z6",
      subtotal, tax, total, new BigDecimal("96.50"),
      InvoiceStatus.EXTRACTED, null, List.of(line), confidence, null, null, null);

    return new InvoiceExtractionResult(invoice, 96.5);
  }
}
