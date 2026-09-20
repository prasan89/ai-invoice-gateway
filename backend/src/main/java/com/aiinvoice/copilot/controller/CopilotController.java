package com.aiinvoice.copilot.controller;

import com.aiinvoice.auth.context.PrincipalContext;
import com.aiinvoice.auth.context.TenantContext;
import com.aiinvoice.copilot.dto.*;
import com.aiinvoice.copilot.service.CopilotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService copilotService;

    @PostMapping("/ask")
    public CopilotSessionDto ask(@RequestBody CopilotAskRequest request) {
        com.aiinvoice.auth.entity.User user = PrincipalContext.get();
        UUID userId = user != null ? user.getId() : UUID.fromString("00000000-0000-0000-0000-000000000001");
        return copilotService.ask(TenantContext.getOrDefault(), userId, request);
    }

    @GetMapping("/sessions")
    public List<CopilotSessionDto> listSessions() {
        return copilotService.listSessions(TenantContext.getOrDefault());
    }

    @GetMapping("/sessions/{id}")
    public CopilotSessionDto getSession(@PathVariable UUID id) {
        return copilotService.getSession(id);
    }
}
