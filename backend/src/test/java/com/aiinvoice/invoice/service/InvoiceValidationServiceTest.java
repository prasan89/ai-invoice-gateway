package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceValidationServiceTest {
  private final InvoiceValidationService service = new InvoiceValidationService();

  @Test
  void validatesCorrectTotals() {
    Invoice invoice = new Invoice();
    invoice.setSubtotal(new BigDecimal("100.00"));
    invoice.setTaxAmount(new BigDecimal("18.00"));
    invoice.setTotalAmount(new BigDecimal("118.00"));
    InvoiceLine line = new InvoiceLine();
    line.setLineTotal(new BigDecimal("118.00"));
    invoice.addLine(line);
    assertNull(service.validate(invoice));
  }

  @Test
  void rejectsIncorrectTotal() {
    Invoice invoice = new Invoice();
    invoice.setSubtotal(new BigDecimal("100.00"));
    invoice.setTaxAmount(new BigDecimal("18.00"));
    invoice.setTotalAmount(new BigDecimal("125.00"));
    InvoiceLine line = new InvoiceLine();
    line.setLineTotal(new BigDecimal("125.00"));
    invoice.addLine(line);
    assertEquals("Subtotal + tax does not match invoice total", service.validate(invoice));
  }
}
