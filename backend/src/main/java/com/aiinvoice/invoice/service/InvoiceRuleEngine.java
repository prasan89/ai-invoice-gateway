package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.domain.GstinValidationStatus;
import com.aiinvoice.invoice.entity.Invoice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceRuleEngine {

    public enum RuleName {
        REQUIRE_SUPPLIER_GSTIN,
        REQUIRE_CUSTOMER_GSTIN,
        REQUIRE_HSN_ON_LINES,
        NO_DUPLICATE,
        GST_VALID,
        ARITHMETIC_VALID
    }

    public record RuleResult(RuleName rule, boolean passed, String message) {}

    public List<RuleResult> evaluate(Invoice invoice) {
        List<RuleResult> results = new ArrayList<>();

        results.add(check(RuleName.REQUIRE_SUPPLIER_GSTIN,
            invoice.getSupplierGstin() != null && !invoice.getSupplierGstin().isBlank(),
            "Supplier GSTIN is required"));

        results.add(check(RuleName.REQUIRE_CUSTOMER_GSTIN,
            invoice.getCustomerGstin() != null && !invoice.getCustomerGstin().isBlank(),
            "Customer GSTIN is required"));

        boolean allHsn = invoice.getLines() != null && !invoice.getLines().isEmpty()
            && invoice.getLines().stream()
                .allMatch(l -> l.getHsnSac() != null && !l.getHsnSac().isBlank());
        results.add(check(RuleName.REQUIRE_HSN_ON_LINES, allHsn,
            "All line items must have an HSN/SAC code"));

        int dupScore = invoice.getDuplicateScore() == null ? 0 : invoice.getDuplicateScore();
        results.add(check(RuleName.NO_DUPLICATE, dupScore < 80,
            "Potential duplicate detected (score: " + dupScore + ")"));

        results.add(check(RuleName.GST_VALID,
            invoice.getSupplierGstinStatus() == GstinValidationStatus.VALID,
            "Supplier GSTIN format is invalid"));

        results.add(check(RuleName.ARITHMETIC_VALID,
            invoice.getArithmeticStatus() == ArithmeticStatus.PASS,
            "Arithmetic validation failed"));

        return results;
    }

    public boolean allPassed(List<RuleResult> results) {
        return results.stream().allMatch(RuleResult::passed);
    }

    private RuleResult check(RuleName rule, boolean condition, String failMessage) {
        return new RuleResult(rule, condition, condition ? "OK" : failMessage);
    }
}
