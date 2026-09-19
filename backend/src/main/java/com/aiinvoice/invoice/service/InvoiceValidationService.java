package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.ValidationResult;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceValidationService {
  private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

  public List<ValidationResult> validate(Invoice invoice) {
    List<ValidationResult> results = new ArrayList<>();

    if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
      results.add(ValidationResult.fail("invoiceNumber", "Invoice number is required"));
      return results;
    }
    results.add(ValidationResult.pass("invoiceNumber", "Invoice number present"));

    if (invoice.getSubtotal() == null || invoice.getTotalAmount() == null) {
      results.add(ValidationResult.fail("subtotal", "Subtotal and total are required"));
      return results;
    }
    results.add(ValidationResult.pass("subtotal", "Subtotal and total present"));

    if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
      results.add(ValidationResult.fail("lines", "At least one invoice line is required"));
      return results;
    }

    boolean linesHaveTotals = true;
    for (InvoiceLine line : invoice.getLines()) {
      if (line.getLineTotal() == null) {
        results.add(ValidationResult.fail("lines", "Every invoice line must have a line total"));
        linesHaveTotals = false;
        break;
      }
    }

    if (linesHaveTotals) {
      BigDecimal lineSum = invoice.getLines().stream()
          .map(InvoiceLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
      if (diff(lineSum, invoice.getTotalAmount())) {
        results.add(ValidationResult.fail("totalAmount",
            "Line totals (" + lineSum.toPlainString() + ") do not match invoice total ("
                + invoice.getTotalAmount().toPlainString() + ")"));
      } else {
        results.add(ValidationResult.pass("totalAmount", "Line totals match invoice total"));
      }
    }

    BigDecimal cgst = nvl(invoice.getCgstAmount());
    BigDecimal sgst = nvl(invoice.getSgstAmount());
    BigDecimal igst = nvl(invoice.getIgstAmount());
    BigDecimal cess = nvl(invoice.getCessAmount());
    BigDecimal derivedTax = cgst.add(sgst).add(igst).add(cess);

    boolean hasBreakdown = cgst.compareTo(BigDecimal.ZERO) > 0
        || sgst.compareTo(BigDecimal.ZERO) > 0
        || igst.compareTo(BigDecimal.ZERO) > 0;

    if (hasBreakdown) {
      if (cgst.compareTo(BigDecimal.ZERO) > 0 && sgst.compareTo(BigDecimal.ZERO) > 0
          && diff(cgst, sgst)) {
        results.add(ValidationResult.fail("cgstAmount",
            "CGST and SGST must be equal for intra-state supply"));
      } else if (cgst.compareTo(BigDecimal.ZERO) > 0 || sgst.compareTo(BigDecimal.ZERO) > 0) {
        results.add(ValidationResult.pass("cgstAmount", "CGST/SGST balance valid"));
      }

      if ((cgst.compareTo(BigDecimal.ZERO) > 0 || sgst.compareTo(BigDecimal.ZERO) > 0)
          && igst.compareTo(BigDecimal.ZERO) > 0) {
        results.add(ValidationResult.fail("igstAmount",
            "CGST/SGST and IGST cannot both be present on the same invoice"));
      }

      if (invoice.getTaxAmount() != null && diff(derivedTax, invoice.getTaxAmount())) {
        results.add(ValidationResult.fail("taxAmount",
            "CGST + SGST + IGST + Cess (" + derivedTax.toPlainString()
                + ") does not match tax amount (" + invoice.getTaxAmount().toPlainString() + ")"));
      } else if (invoice.getTaxAmount() != null) {
        results.add(ValidationResult.pass("taxAmount", "GST breakdown matches tax total"));
      }
    }

    BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : derivedTax;
    BigDecimal expected = invoice.getSubtotal().add(tax).setScale(2, RoundingMode.HALF_UP);
    if (diff(expected, invoice.getTotalAmount())) {
      results.add(ValidationResult.fail("totalAmount",
          "Subtotal + tax (" + expected.toPlainString()
              + ") does not match invoice total (" + invoice.getTotalAmount().toPlainString() + ")"));
    } else {
      results.add(ValidationResult.pass("totalAmount", "Subtotal + tax matches invoice total"));
    }

    return results;
  }

  public boolean hasFailures(List<ValidationResult> results) {
    return results.stream().anyMatch(ValidationResult::isFailed);
  }

  private boolean diff(BigDecimal a, BigDecimal b) {
    return a.subtract(b).abs().compareTo(TOLERANCE) > 0;
  }

  private BigDecimal nvl(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
