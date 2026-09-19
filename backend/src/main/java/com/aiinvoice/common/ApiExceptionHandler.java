package com.aiinvoice.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("TRANSITION_INVALID", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
        .toList();
    return ResponseEntity.badRequest()
        .body(ErrorResponse.withDetails("VALIDATION_FAILED", "Request validation failed", details));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("INTERNAL_SERVER_ERROR",
            ex.getMessage() != null ? ex.getMessage() : "Unexpected server error"));
  }
}
