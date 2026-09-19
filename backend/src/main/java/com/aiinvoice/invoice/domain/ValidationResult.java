package com.aiinvoice.invoice.domain;

public record ValidationResult(String field, RuleStatus status, String message) {

    public enum RuleStatus { PASS, FAIL }

    public static ValidationResult pass(String field, String message) {
        return new ValidationResult(field, RuleStatus.PASS, message);
    }

    public static ValidationResult fail(String field, String message) {
        return new ValidationResult(field, RuleStatus.FAIL, message);
    }

    public boolean isFailed() {
        return status == RuleStatus.FAIL;
    }
}
