package com.aiinvoice.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    String code,
    String message,
    List<FieldError> details
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(Instant.now(), code, message, List.of());
    }

    public static ErrorResponse withDetails(String code, String message, List<FieldError> details) {
        return new ErrorResponse(Instant.now(), code, message, details);
    }
}
