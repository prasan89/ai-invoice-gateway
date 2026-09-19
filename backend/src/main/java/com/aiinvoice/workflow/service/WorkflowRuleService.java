package com.aiinvoice.workflow.service;

import com.aiinvoice.workflow.dto.WorkflowRuleDto;
import com.aiinvoice.workflow.entity.WorkflowRule;
import com.aiinvoice.workflow.repository.WorkflowRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowRuleService {

    private final WorkflowRuleRepository ruleRepo;

    public List<WorkflowRuleDto> listByOrg(UUID organizationId) {
        return ruleRepo.findByOrganizationIdAndActiveTrueOrderByPriorityAsc(organizationId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public WorkflowRuleDto create(UUID organizationId, WorkflowRuleDto req) {
        WorkflowRule rule = new WorkflowRule();
        rule.setId(UUID.randomUUID());
        rule.setOrganizationId(organizationId);
        rule.setName(req.name());
        rule.setPriority(req.priority());
        rule.setConditions(req.conditions());
        rule.setAction(req.action());
        rule.setRequiredRole(req.requiredRole());
        rule.setNextState(req.nextState());
        rule.setActive(true);
        rule.setCreatedAt(Instant.now());
        return toDto(ruleRepo.save(rule));
    }

    @Transactional
    public void delete(UUID organizationId, UUID id) {
        ruleRepo.findById(id).filter(r -> organizationId.equals(r.getOrganizationId())).ifPresent(r -> {
            r.setActive(false);
            ruleRepo.save(r);
        });
    }

    private WorkflowRuleDto toDto(WorkflowRule r) {
        return new WorkflowRuleDto(r.getId(), r.getName(), r.getPriority(), r.getConditions(),
                r.getAction(), r.getRequiredRole(), r.getNextState(), r.isActive(), r.getCreatedAt());
    }
}
