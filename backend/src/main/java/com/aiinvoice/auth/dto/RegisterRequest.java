package com.aiinvoice.auth.dto;

public record RegisterRequest(String email, String password, String role) {
}
