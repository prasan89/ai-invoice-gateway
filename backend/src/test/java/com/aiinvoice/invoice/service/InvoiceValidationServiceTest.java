package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.ValidationResult;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceValidationServiceTest {

    private InvoiceValidationService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceValidationService();
    }

    private Invoice baseInvoice() {
        Invoice inv = new Invoice();
        inv.setId(UUID.randomUUID());
        inv.setInvoiceNumber("INV-001");
        inv.setSubtotal(new BigDecimal("100.00"));
        inv.setTotalAmount(new BigDecimal("118.00"));
        inv.setTaxAmount(new BigDecimal("18.00"));
        InvoiceLine line = new InvoiceLine();
        line.setId(UUID.randomUUID());
        line.setLineTotal(new BigDecimal("118.00"));
        inv.addLine(line);
        return inv;
    }

    @Test
    void validInvoice_noFailures() {
        assertFalse(service.hasFailures(service.validate(baseInvoice())));
    }

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

    @Test
    void missingInvoiceNumber_fails() {
        Invoice inv = baseInvoice();
        inv.setInvoiceNumber(null);
        List<ValidationResult> results = service.validate(inv);
        assertTrue(service.hasFailures(results));
        assertTrue(results.stream().anyMatch(r -> r.field().equals("invoiceNumber")));
    }

    @Test
    void blankInvoiceNumber_fails() {
        Invoice inv = baseInvoice();
        inv.setInvoiceNumber("   ");
        assertTrue(service.hasFailures(service.validate(inv)));
    }

    @Test
    void missingSubtotal_fails() {
        Invoice inv = baseInvoice();
        inv.setSubtotal(null);
        List<ValidationResult> results = service.validate(inv);
        assertTrue(service.hasFailures(results));
        assertTrue(results.stream().anyMatch(r -> r.field().equals("subtotal")));
    }

    @Test
    void emptyLines_fails() {
        Invoice inv = baseInvoice();
        inv.getLines().clear();
        List<ValidationResult> results = service.validate(inv);
        assertTrue(service.hasFailures(results));
        assertTrue(results.stream().anyMatch(r -> r.field().equals("lines")));
    }

    @Test
    void lineWithNullTotal_fails() {
        Invoice inv = baseInvoice();
        inv.getLines().get(0).setLineTotal(null);
        assertTrue(service.hasFailures(service.validate(inv)));
    }

    @Test
    void lineTotalMismatch_fails() {
        Invoice inv = baseInvoice();
        inv.getLines().get(0).setLineTotal(new BigDecimal("200.00"));
        assertTrue(service.hasFailures(service.validate(inv)));
    }

    @Test
    void lineTotalWithinTolerance_passes() {
        Invoice inv = baseInvoice();
        inv.getLines().get(0).setLineTotal(new BigDecimal("118.01")); // within 0.02
        assertFalse(service.hasFailures(service.validate(inv)));
    }

    @Test
    void cgstSgstMismatch_fails() {
        Invoice inv = baseInvoice();
        inv.setCgstAmount(new BigDecimal("9.00"));
        inv.setSgstAmount(new BigDecimal("10.00")); // must be equal for intra-state
        List<ValidationResult> results = service.validate(inv);
        assertTrue(results.stream().anyMatch(r -> r.field().equals("cgstAmount") && r.isFailed()));
    }

    @Test
    void cgstSgstEqual_passes() {
        Invoice inv = baseInvoice();
        inv.setCgstAmount(new BigDecimal("9.00"));
        inv.setSgstAmount(new BigDecimal("9.00"));
        inv.setTaxAmount(new BigDecimal("18.00"));
        List<ValidationResult> results = service.validate(inv);
        assertFalse(results.stream().anyMatch(r -> r.field().equals("cgstAmount") && r.isFailed()));
    }

    @Test
    void cgstAndIgstBothPresent_fails() {
        Invoice inv = baseInvoice();
        inv.setCgstAmount(new BigDecimal("9.00"));
        inv.setSgstAmount(new BigDecimal("9.00"));
        inv.setIgstAmount(new BigDecimal("18.00"));
        List<ValidationResult> results = service.validate(inv);
        assertTrue(results.stream().anyMatch(r -> r.field().equals("igstAmount") && r.isFailed()));
    }

    @Test
    void taxBreakdownMismatch_fails() {
        Invoice inv = baseInvoice();
        inv.setCgstAmount(new BigDecimal("9.00"));
        inv.setSgstAmount(new BigDecimal("9.00"));
        inv.setTaxAmount(new BigDecimal("20.00")); // 9+9=18 ≠ 20
        List<ValidationResult> results = service.validate(inv);
        assertTrue(results.stream().anyMatch(r -> r.field().equals("taxAmount") && r.isFailed()));
    }

    @Test
    void hasFailures_withNoResults_returnsFalse() {
        assertFalse(service.hasFailures(List.of()));
    }

    @Test
    void hasFailures_withAllPass_returnsFalse() {
        assertFalse(service.hasFailures(List.of(
            ValidationResult.pass("f1", "ok"),
            ValidationResult.pass("f2", "ok")
        )));
    }

    @Test
    void hasFailures_withOneFail_returnsTrue() {
        assertTrue(service.hasFailures(List.of(
            ValidationResult.pass("f1", "ok"),
            ValidationResult.fail("f2", "bad")
        )));
    }
}
