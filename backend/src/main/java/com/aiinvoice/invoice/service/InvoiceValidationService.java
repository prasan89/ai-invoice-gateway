package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoiceValidationService {
  private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

  public String validate(Invoice invoice) {
    if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank())
      return "Invoice number is required";
    if (invoice.getSubtotal() == null || invoice.getTotalAmount() == null)
      return "Subtotal and total are required";
    if (invoice.getLines() == null || invoice.getLines().isEmpty())
      return "At least one invoice line is required";

    // Validate each line has a total
    for (InvoiceLine line : invoice.getLines()) {
      if (line.getLineTotal() == null) return "Every invoice line must have a line total";
    }

    // Sum of line totals must equal invoice total
    BigDecimal lineSum = invoice.getLines().stream()
        .map(InvoiceLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (diff(lineSum, invoice.getTotalAmount()))
      return "Line totals (" + lineSum.toPlainString() + ") do not match invoice total (" + invoice.getTotalAmount().toPlainString() + ")";

    // GST breakdown: if any GST component is present, validate the chain
    BigDecimal cgst = nvl(invoice.getCgstAmount());
    BigDecimal sgst = nvl(invoice.getSgstAmount());
    BigDecimal igst = nvl(invoice.getIgstAmount());
    BigDecimal cess = nvl(invoice.getCessAmount());
    BigDecimal derivedTax = cgst.add(sgst).add(igst).add(cess);

    boolean hasBreakdown = cgst.compareTo(BigDecimal.ZERO) > 0
        || sgst.compareTo(BigDecimal.ZERO) > 0
        || igst.compareTo(BigDecimal.ZERO) > 0;

    if (hasBreakdown) {
      // CGST and SGST must be equal for intra-state
      if (cgst.compareTo(BigDecimal.ZERO) > 0 && sgst.compareTo(BigDecimal.ZERO) > 0
          && diff(cgst, sgst))
        return "CGST and SGST must be equal for intra-state supply";

      // CGST+SGST and IGST cannot both be non-zero
      if ((cgst.compareTo(BigDecimal.ZERO) > 0 || sgst.compareTo(BigDecimal.ZERO) > 0)
          && igst.compareTo(BigDecimal.ZERO) > 0)
        return "CGST/SGST and IGST cannot both be present on the same invoice";

      // Tax breakdown must match reported taxAmount (if present)
      if (invoice.getTaxAmount() != null && diff(derivedTax, invoice.getTaxAmount()))
        return "CGST + SGST + IGST + Cess (" + derivedTax.toPlainString()
            + ") does not match tax amount (" + invoice.getTaxAmount().toPlainString() + ")";
    }

    // subtotal + total tax = grand total
    BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : derivedTax;
    BigDecimal expected = invoice.getSubtotal().add(tax).setScale(2, RoundingMode.HALF_UP);
    if (diff(expected, invoice.getTotalAmount()))
      return "Subtotal + tax (" + expected.toPlainString()
          + ") does not match invoice total (" + invoice.getTotalAmount().toPlainString() + ")";

    return null;
  }

  private boolean diff(BigDecimal a, BigDecimal b) {
    return a.subtract(b).abs().compareTo(TOLERANCE) > 0;
  }

  private BigDecimal nvl(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
