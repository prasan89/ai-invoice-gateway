package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceArithmeticService {

    private final BigDecimal tolerance;
    private final BigDecimal warnThreshold;

    public InvoiceArithmeticService(
            @Value("${invoice.arithmetic.tolerance:0.05}") BigDecimal tolerance,
            @Value("${invoice.arithmetic.warn-threshold:0.50}") BigDecimal warnThreshold) {
        this.tolerance = tolerance;
        this.warnThreshold = warnThreshold;
    }

    public record LineDiscrepancy(int lineIndex, String field, BigDecimal expected, BigDecimal actual) {}

    public record ArithmeticCheckResult(ArithmeticStatus status, List<LineDiscrepancy> discrepancies) {}

    public ArithmeticCheckResult check(Invoice invoice) {
        if (invoice.getLines() == null || invoice.getLines().isEmpty()) {
            return new ArithmeticCheckResult(ArithmeticStatus.PENDING, List.of());
        }

        List<LineDiscrepancy> issues = new ArrayList<>();
        BigDecimal maxDiscrepancy = BigDecimal.ZERO;
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
                if (line.getTaxableValue() != null) {
                    BigDecimal d = absGap(expectedTaxable, line.getTaxableValue());
                    if (d.compareTo(tolerance) > 0) {
                        issues.add(new LineDiscrepancy(i, "taxableValue", expectedTaxable, line.getTaxableValue()));
                        maxDiscrepancy = maxDiscrepancy.max(d);
                    }
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
                if (line.getLineTotal() != null) {
                    BigDecimal d = absGap(expectedTotal, line.getLineTotal());
                    if (d.compareTo(tolerance) > 0) {
                        issues.add(new LineDiscrepancy(i, "lineTotal", expectedTotal, line.getLineTotal()));
                        maxDiscrepancy = maxDiscrepancy.max(d);
                    }
                }
            }

            if (line.getLineTotal() != null) {
                lineSum = lineSum.add(line.getLineTotal());
            }
        }

        // Invoice total vs sum of line totals
        if (invoice.getTotalAmount() != null) {
            BigDecimal d = absGap(lineSum, invoice.getTotalAmount());
            if (d.compareTo(tolerance) > 0) {
                issues.add(new LineDiscrepancy(-1, "totalAmount", lineSum, invoice.getTotalAmount()));
                maxDiscrepancy = maxDiscrepancy.max(d);
            }
        }

        // subtotal + taxAmount vs totalAmount
        if (invoice.getSubtotal() != null && invoice.getTaxAmount() != null && invoice.getTotalAmount() != null) {
            BigDecimal expectedTotal = invoice.getSubtotal().add(invoice.getTaxAmount())
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal d = absGap(expectedTotal, invoice.getTotalAmount());
            if (d.compareTo(tolerance) > 0) {
                issues.add(new LineDiscrepancy(-1, "subtotal+tax", expectedTotal, invoice.getTotalAmount()));
                maxDiscrepancy = maxDiscrepancy.max(d);
            }
        }

        if (issues.isEmpty()) return new ArithmeticCheckResult(ArithmeticStatus.PASS, List.of());
        ArithmeticStatus status = maxDiscrepancy.compareTo(warnThreshold) <= 0
            ? ArithmeticStatus.WARN : ArithmeticStatus.FAIL;
        return new ArithmeticCheckResult(status, issues);
    }

    private BigDecimal absGap(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
