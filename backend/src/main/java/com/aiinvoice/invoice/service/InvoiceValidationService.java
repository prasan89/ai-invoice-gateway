package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoiceValidationService {
  public String validate(Invoice invoice) {
    BigDecimal lineSum = invoice.getLines().stream()
      .map(InvoiceLine::getLineTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (lineSum.subtract(invoice.getTotalAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
      return "Line totals do not match invoice total";
    }
    BigDecimal expected = invoice.getSubtotal().add(invoice.getTaxAmount())
      .setScale(2, RoundingMode.HALF_UP);
    if (expected.subtract(invoice.getTotalAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
      return "Subtotal + tax does not match invoice total";
    }
    return null;
  }
}
