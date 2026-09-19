package com.aiinvoice.auth.dto;

import java.util.UUID;

public record LoginResponse(String sessionToken, UUID userId, String email, String role, UUID organizationId) {
}
