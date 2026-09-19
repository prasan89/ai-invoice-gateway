package com.aiinvoice.webhook.controller;

import com.aiinvoice.webhook.dto.CreateWebhookRequest;
import com.aiinvoice.webhook.dto.WebhookDto;
import com.aiinvoice.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookService webhookService;
    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping
    public List<WebhookDto> list() {
        return webhookService.listByOrg(DEMO_ORG);
    }

    @PostMapping
    public ResponseEntity<WebhookDto> create(@RequestBody CreateWebhookRequest req) {
        return ResponseEntity.ok(webhookService.create(DEMO_ORG, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
