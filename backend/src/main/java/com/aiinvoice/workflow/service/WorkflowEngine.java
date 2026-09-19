package com.aiinvoice.workflow.service;

import com.aiinvoice.workflow.dto.WorkflowDecision;
import com.aiinvoice.workflow.entity.WorkflowRule;
import com.aiinvoice.workflow.repository.WorkflowRuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowRuleRepository ruleRepo;
    private final ObjectMapper objectMapper;

    public Optional<WorkflowDecision> evaluate(UUID organizationId, Map<String, Object> invoiceFields) {
        List<WorkflowRule> rules = ruleRepo.findByOrganizationIdAndActiveTrueOrderByPriorityAsc(organizationId);
        for (WorkflowRule rule : rules) {
            if (matches(rule.getConditions(), invoiceFields)) {
                return Optional.of(new WorkflowDecision(rule.getNextState(), rule.getRequiredRole(), rule.getName()));
            }
        }
        return Optional.empty();
    }

    private boolean matches(String conditionsJson, Map<String, Object> fields) {
        try {
            List<Map<String, String>> conditions = objectMapper.readValue(conditionsJson,
                    new TypeReference<>() {});
            for (Map<String, String> cond : conditions) {
                if (!evaluateCondition(cond, fields)) return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse workflow conditions: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateCondition(Map<String, String> cond, Map<String, Object> fields) {
        String field = cond.get("field");
        String op = cond.get("operator");
        String value = cond.get("value");
        Object actual = fields.get(field);
        if (actual == null) return false;

        return switch (op) {
            case "gt" -> toDecimal(actual).compareTo(new BigDecimal(value)) > 0;
            case "gte" -> toDecimal(actual).compareTo(new BigDecimal(value)) >= 0;
            case "lt" -> toDecimal(actual).compareTo(new BigDecimal(value)) < 0;
            case "lte" -> toDecimal(actual).compareTo(new BigDecimal(value)) <= 0;
            case "eq" -> actual.toString().equals(value);
            case "neq" -> !actual.toString().equals(value);
            case "contains" -> actual.toString().contains(value);
            default -> false;
        };
    }

    private BigDecimal toDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }
}
