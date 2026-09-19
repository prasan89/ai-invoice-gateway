package com.aiinvoice.apikey.controller;

import com.aiinvoice.apikey.dto.ApiKeyDto;
import com.aiinvoice.apikey.dto.CreateApiKeyRequest;
import com.aiinvoice.apikey.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping
    public List<ApiKeyDto> list() {
        return apiKeyService.listByOrg(DEMO_ORG);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateApiKeyRequest req) {
        ApiKeyService.CreatedKey result = apiKeyService.create(DEMO_ORG, req);
        return ResponseEntity.ok(Map.of("key", result.rawKey(), "meta", result.dto()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        apiKeyService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
