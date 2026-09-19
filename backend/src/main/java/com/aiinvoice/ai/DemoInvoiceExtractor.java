package com.aiinvoice.ai;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DemoInvoiceExtractor implements InvoiceExtractor {
  @Override
  public InvoiceExtractionResult extract(MultipartFile document) {
    BigDecimal subtotal = new BigDecimal("100000.00");
    BigDecimal tax = new BigDecimal("18000.00");
    BigDecimal total = new BigDecimal("118000.00");
    InvoiceLineDto line = new InvoiceLineDto(null, "Software Services", BigDecimal.ONE,
      subtotal, BigDecimal.ZERO, new BigDecimal("18"), tax, total);
    InvoiceDto invoice = new InvoiceDto(null, "DEMO-INV-1001", LocalDate.now(), "INR",
      "Demo Supplier Pvt Ltd", "29AAAAA0000A1Z5",
      "Demo Customer Pvt Ltd", "33BBBBB0000B1Z6",
      subtotal, tax, total, new BigDecimal("96.50"),
      InvoiceStatus.EXTRACTED, null, List.of(line));
    return new InvoiceExtractionResult(invoice, 96.5);
  }
}
