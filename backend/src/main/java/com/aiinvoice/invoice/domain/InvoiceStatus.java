package com.aiinvoice.invoice.domain;

import java.util.Set;

public enum InvoiceStatus {
    UPLOADED, PROCESSING, EXTRACTED, REVIEW_REQUIRED, APPROVED, AUTO_APPROVED, FAILED, REJECTED;

    private static final Set<InvoiceStatus> TERMINAL = Set.of(APPROVED, AUTO_APPROVED, REJECTED);

    public InvoiceStatus transitionTo(InvoiceStatus next) {
        if (TERMINAL.contains(this)) {
            throw new IllegalStateException(
                "Invoice in status " + this + " cannot transition to " + next + " (terminal state)");
        }
        boolean valid = switch (this) {
            case UPLOADED -> next == PROCESSING || next == FAILED;
            case PROCESSING -> next == EXTRACTED || next == FAILED;
            case EXTRACTED -> next == REVIEW_REQUIRED || next == FAILED;
            case REVIEW_REQUIRED -> next == REVIEW_REQUIRED || next == APPROVED
                || next == AUTO_APPROVED || next == REJECTED || next == FAILED;
            case FAILED -> next == REVIEW_REQUIRED || next == REJECTED;
            default -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                "Invalid status transition: " + this + " → " + next);
        }
        return next;
    }
}
