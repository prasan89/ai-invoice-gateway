package com.aiinvoice.workflow.dto;

public record WorkflowDecision(String nextState, String requiredRole, String ruleName) {
}
