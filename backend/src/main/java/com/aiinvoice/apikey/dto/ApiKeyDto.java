package com.aiinvoice.apikey.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyDto(UUID id, String name, List<String> scopes, Instant createdAt, Instant lastUsedAt) {
}
