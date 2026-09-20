package com.aiinvoice.copilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CopilotSessionDto(UUID id, String title, Instant updatedAt, List<CopilotMessageDto> messages) {}
