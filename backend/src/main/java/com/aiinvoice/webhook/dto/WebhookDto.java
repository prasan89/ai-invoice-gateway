package com.aiinvoice.webhook.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookDto(UUID id, String url, List<String> events, boolean active, Instant createdAt) {
}
