package com.aiinvoice.workflow.repository;

import com.aiinvoice.workflow.entity.WorkflowRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkflowRuleRepository extends JpaRepository<WorkflowRule, UUID> {
    List<WorkflowRule> findByOrganizationIdAndActiveTrueOrderByPriorityAsc(UUID organizationId);
}
