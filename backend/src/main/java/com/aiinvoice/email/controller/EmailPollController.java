package com.aiinvoice.email.controller;

import com.aiinvoice.email.entity.EmailPollLog;
import com.aiinvoice.email.repository.EmailPollLogRepository;
import com.aiinvoice.email.service.EmailPollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/email-poll")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class EmailPollController {

    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final EmailPollLogRepository logRepository;
    private final EmailPollService emailPollService;

    @GetMapping("/logs")
    public ResponseEntity<List<EmailPollLog>> logs() {
        return ResponseEntity.ok(logRepository.findTop20ByOrganizationIdOrderByPolledAtDesc(DEMO_ORG));
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> trigger() {
        emailPollService.poll();
        return ResponseEntity.ok(Map.of("status", "triggered"));
    }
}
