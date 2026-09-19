package com.aiinvoice.workflow.controller;

import com.aiinvoice.workflow.dto.WorkflowRuleDto;
import com.aiinvoice.workflow.service.WorkflowRuleService;
import com.aiinvoice.auth.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflow-rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowRuleController {

    private final WorkflowRuleService ruleService;
    @GetMapping
    public List<WorkflowRuleDto> list() {
        return ruleService.listByOrg(TenantContext.getOrDefault());
    }

    @PostMapping
    public ResponseEntity<WorkflowRuleDto> create(@RequestBody WorkflowRuleDto req) {
        return ResponseEntity.ok(ruleService.create(TenantContext.getOrDefault(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ruleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
