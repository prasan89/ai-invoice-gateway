package com.aiinvoice.erp.controller;

import com.aiinvoice.erp.entity.ErpSyncJob;
import com.aiinvoice.erp.service.ErpSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ErpController {

    private final ErpSyncService erpSyncService;

    @GetMapping("/{id}/erp-status")
    public List<ErpSyncJob> erpStatus(@PathVariable UUID id) {
        return erpSyncService.getByInvoice(id);
    }

    @PostMapping("/{id}/erp-push")
    public ErpSyncJob erpPush(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        return erpSyncService.sync(id, payload);
    }
}
