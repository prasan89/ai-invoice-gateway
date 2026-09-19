package com.aiinvoice.workflow.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkflowRuleDto(UUID id, String name, int priority, String conditions,
                              String action, String requiredRole, String nextState,
                              boolean active, Instant createdAt) {
}
