package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceArithmeticService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.05");

    public record LineDiscrepancy(int lineIndex, String field, BigDecimal expected, BigDecimal actual) {}

    public record ArithmeticCheckResult(ArithmeticStatus status, List<LineDiscrepancy> discrepancies) {}

    public ArithmeticCheckResult check(Invoice invoice) {
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            return new ArithmeticCheckResult(ArithmeticStatus.PENDING, List.of());
        }

        List<LineDiscrepancy> issues = new ArrayList<>();
        BigDecimal lineSum = BigDecimal.ZERO;

        List<InvoiceLine> lines = invoice.getLines();
        for (int i = 0; i < lines.size(); i++) {
            InvoiceLine line = lines.get(i);

            // Recalculate taxable value: qty * unitPrice - discount
            if (line.getQuantity() != null && line.getUnitPrice() != null) {
                BigDecimal discount = nvl(line.getDiscount());
                BigDecimal expectedTaxable = line.getQuantity()
                    .multiply(line.getUnitPrice()).subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);
                if (line.getTaxableValue() != null && diff(expectedTaxable, line.getTaxableValue())) {
                    issues.add(new LineDiscrepancy(i, "taxableValue", expectedTaxable, line.getTaxableValue()));
                }
            }

            // Recalculate line total: taxableValue + all tax components
            if (line.getTaxableValue() != null) {
                BigDecimal expectedTotal = line.getTaxableValue()
                    .add(nvl(line.getCgstAmount()))
                    .add(nvl(line.getSgstAmount()))
                    .add(nvl(line.getIgstAmount()))
                    .add(nvl(line.getCessAmount()))
                    .setScale(2, RoundingMode.HALF_UP);
                if (line.getLineTotal() != null && diff(expectedTotal, line.getLineTotal())) {
                    issues.add(new LineDiscrepancy(i, "lineTotal", expectedTotal, line.getLineTotal()));
                }
            }

            if (line.getLineTotal() != null) {
                lineSum = lineSum.add(line.getLineTotal());
            }
        }

        // Invoice total vs sum of line totals
        if (invoice.getTotalAmount() != null && diff(lineSum, invoice.getTotalAmount())) {
            issues.add(new LineDiscrepancy(-1, "totalAmount", lineSum, invoice.getTotalAmount()));
        }

        // subtotal + taxAmount vs totalAmount
        if (invoice.getSubtotal() != null && invoice.getTaxAmount() != null && invoice.getTotalAmount() != null) {
            BigDecimal expectedTotal = invoice.getSubtotal().add(invoice.getTaxAmount())
                .setScale(2, RoundingMode.HALF_UP);
            if (diff(expectedTotal, invoice.getTotalAmount())) {
                issues.add(new LineDiscrepancy(-1, "subtotal+tax", expectedTotal, invoice.getTotalAmount()));
            }
        }

        ArithmeticStatus status = issues.isEmpty() ? ArithmeticStatus.PASS : ArithmeticStatus.FAIL;
        return new ArithmeticCheckResult(status, issues);
    }

    private boolean diff(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(TOLERANCE) > 0;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
