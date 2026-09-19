package com.aiinvoice.workflow.controller;

import com.aiinvoice.workflow.dto.WorkflowRuleDto;
import com.aiinvoice.workflow.service.WorkflowRuleService;
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
    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping
    public List<WorkflowRuleDto> list() {
        return ruleService.listByOrg(DEMO_ORG);
    }

    @PostMapping
    public ResponseEntity<WorkflowRuleDto> create(@RequestBody WorkflowRuleDto req) {
        return ResponseEntity.ok(ruleService.create(DEMO_ORG, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ruleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
