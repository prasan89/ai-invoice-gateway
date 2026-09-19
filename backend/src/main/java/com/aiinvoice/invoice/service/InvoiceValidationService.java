package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoiceValidationService {
  public String validate(Invoice invoice) {
    if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
      return "Invoice number is required";
    }
    if (invoice.getSubtotal() == null || invoice.getTaxAmount() == null || invoice.getTotalAmount() == null) {
      return "Subtotal, tax and total are required";
    }
    if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
      return "At least one invoice line is required";
    }

    BigDecimal lineSum = BigDecimal.ZERO;
    for (InvoiceLine line : invoice.getLines()) {
      if (line.getLineTotal() == null) return "Every invoice line must have a line total";
      lineSum = lineSum.add(line.getLineTotal());
    }

    if (lineSum.subtract(invoice.getTotalAmount()).abs()
        .compareTo(new BigDecimal("0.01")) > 0) {
      return "Line totals do not match invoice total";
    }

    BigDecimal expected = invoice.getSubtotal().add(invoice.getTaxAmount())
        .setScale(2, RoundingMode.HALF_UP);
    if (expected.subtract(invoice.getTotalAmount()).abs()
        .compareTo(new BigDecimal("0.01")) > 0) {
      return "Subtotal + tax does not match invoice total";
    }

    return null;
  }
}
