package com.aiinvoice.erp.controller;

import com.aiinvoice.auth.context.TenantContext;
import com.aiinvoice.erp.entity.ErpConnection;
import com.aiinvoice.erp.entity.ErpSyncJob;
import com.aiinvoice.erp.service.ErpSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ErpController {

    private final ErpSyncService erpSyncService;

    @GetMapping("/api/v1/invoices/{id}/erp-status")
    public List<ErpSyncJob> erpStatus(@PathVariable UUID id) {
        return erpSyncService.getByInvoice(id);
    }

    @PostMapping("/api/v1/invoices/{id}/erp-push")
    public ErpSyncJob erpPush(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        return erpSyncService.sync(TenantContext.getOrDefault(), id, payload);
    }

    @PostMapping("/api/v1/invoices/{id}/erp-push-async")
    public Map<String, String> erpPushAsync(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        erpSyncService.syncAsync(TenantContext.getOrDefault(), id, payload);
        return Map.of("status", "queued", "invoiceId", id.toString());
    }

    @GetMapping("/api/v1/erp/connections")
    public List<ErpConnection> listConnections() {
        return erpSyncService.listConnections(TenantContext.getOrDefault());
    }

    @PostMapping("/api/v1/erp/connections")
    public ErpConnection saveConnection(@RequestBody Map<String, Object> req) {
        UUID orgId = TenantContext.getOrDefault();
        String erpSystem = (String) req.getOrDefault("erpSystem", "GENERIC_REST");
        String displayName = (String) req.getOrDefault("displayName", erpSystem);
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) req.getOrDefault("config", Map.of());
        return erpSyncService.saveConnection(orgId, erpSystem, displayName, config);
    }
}
