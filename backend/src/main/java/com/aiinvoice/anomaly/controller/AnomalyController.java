package com.aiinvoice.anomaly.controller;

import com.aiinvoice.anomaly.dto.AnomalyDto;
import com.aiinvoice.anomaly.dto.ReviewAnomalyRequest;
import com.aiinvoice.anomaly.service.AnomalyDetectionService;
import com.aiinvoice.auth.context.PrincipalContext;
import com.aiinvoice.auth.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyDetectionService anomalyService;

    @GetMapping
    public List<AnomalyDto> listAnomalies() {
        return anomalyService.listByOrg(TenantContext.get());
    }

    @GetMapping("/invoice/{invoiceId}")
    public AnomalyDto getByInvoice(@PathVariable UUID invoiceId) {
        return anomalyService.findByInvoice(invoiceId)
            .orElseThrow(() -> new java.util.NoSuchElementException("No anomaly record for invoice " + invoiceId));
    }

    @PostMapping("/{id}/review")
    public AnomalyDto review(@PathVariable UUID id, @RequestBody ReviewAnomalyRequest req) {
        com.aiinvoice.auth.entity.User user = PrincipalContext.get();
        UUID reviewerId = user != null ? user.getId() : null;
        return anomalyService.reviewAnomaly(id, reviewerId, req);
    }
}
