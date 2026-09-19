package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.ValidationResult;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceValidationServiceTest {
  private final InvoiceValidationService service = new InvoiceValidationService();

  @Test
  void validatesCorrectTotals() {
    Invoice invoice = new Invoice();
    invoice.setInvoiceNumber("INV-001");
    invoice.setSubtotal(new BigDecimal("100.00"));
    invoice.setTaxAmount(new BigDecimal("18.00"));
    invoice.setTotalAmount(new BigDecimal("118.00"));
    InvoiceLine line = new InvoiceLine();
    line.setLineTotal(new BigDecimal("118.00"));
    invoice.addLine(line);
    List<ValidationResult> results = service.validate(invoice);
    assertTrue(results.stream().noneMatch(ValidationResult::isFailed),
        "Expected no failures but got: " + results.stream()
            .filter(ValidationResult::isFailed).map(ValidationResult::message).toList());
  }

  @Test
  void rejectsIncorrectTotal() {
    Invoice invoice = new Invoice();
    invoice.setInvoiceNumber("INV-002");
    invoice.setSubtotal(new BigDecimal("100.00"));
    invoice.setTaxAmount(new BigDecimal("18.00"));
    invoice.setTotalAmount(new BigDecimal("125.00"));
    InvoiceLine line = new InvoiceLine();
    line.setLineTotal(new BigDecimal("125.00"));
    invoice.addLine(line);
    List<ValidationResult> results = service.validate(invoice);
    assertTrue(results.stream().anyMatch(r ->
        r.isFailed() && r.message().contains("does not match invoice total")),
        "Expected a failure about totals not matching");
  }
}
