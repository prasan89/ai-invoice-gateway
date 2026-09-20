package com.aiinvoice.copilot.dto;

import java.time.Instant;
import java.util.UUID;

public record CopilotMessageDto(UUID id, String role, String content, Instant createdAt) {}
