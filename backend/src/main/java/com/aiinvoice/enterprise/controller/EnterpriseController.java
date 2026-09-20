package com.aiinvoice.enterprise.controller;

import com.aiinvoice.auth.context.TenantContext;
import com.aiinvoice.enterprise.entity.*;
import com.aiinvoice.enterprise.service.EnterpriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    // SSO
    @GetMapping("/sso")
    public ResponseEntity<SsoConfiguration> getSso() {
        return enterpriseService.getSsoConfig(TenantContext.getOrDefault())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sso")
    public SsoConfiguration saveSso(@RequestBody Map<String, Object> req) {
        return enterpriseService.saveSsoConfig(
            TenantContext.getOrDefault(),
            (String) req.getOrDefault("provider", "OIDC"),
            (String) req.get("entityId"),
            (String) req.get("metadataUrl"),
            (String) req.get("clientId"),
            (String) req.get("clientSecret"),
            (String) req.get("issuer"),
            Boolean.TRUE.equals(req.get("enabled"))
        );
    }

    // Approval chains
    @GetMapping("/approval-chains")
    public List<ApprovalChain> listChains() {
        return enterpriseService.listChains(TenantContext.getOrDefault());
    }

    @PostMapping("/approval-chains")
    public ApprovalChain saveChain(@RequestBody Map<String, Object> req) {
        UUID chainId = req.get("id") != null ? UUID.fromString((String) req.get("id")) : null;
        BigDecimal minAmt = req.get("minAmount") != null ? new BigDecimal(req.get("minAmount").toString()) : null;
        BigDecimal maxAmt = req.get("maxAmount") != null ? new BigDecimal(req.get("maxAmount").toString()) : null;
        return enterpriseService.saveChain(TenantContext.getOrDefault(), chainId,
            (String) req.get("name"), (String) req.get("description"), minAmt, maxAmt,
            req.get("steps") != null ? req.get("steps").toString() : "[]");
    }

    @PostMapping("/approval-chains/{chainId}/initiate/{invoiceId}")
    public ApprovalRequest initiateApproval(@PathVariable UUID chainId, @PathVariable UUID invoiceId) {
        return enterpriseService.initiateApproval(TenantContext.getOrDefault(), invoiceId, chainId);
    }

    @PostMapping("/approvals/{requestId}/advance")
    public ApprovalRequest advance(@PathVariable UUID requestId, @RequestBody Map<String, Object> req) {
        String outcome = (String) req.getOrDefault("outcome", "APPROVED");
        return enterpriseService.advanceApproval(requestId, outcome, null);
    }

    @GetMapping("/approvals/pending")
    public List<ApprovalRequest> pendingApprovals() {
        return enterpriseService.listPendingApprovals(TenantContext.getOrDefault());
    }

    // Data retention
    @GetMapping("/retention")
    public DataRetentionPolicy getRetention() {
        return enterpriseService.getRetentionPolicy(TenantContext.getOrDefault());
    }

    @PostMapping("/retention")
    public DataRetentionPolicy saveRetention(@RequestBody Map<String, Object> req) {
        int inv = req.get("invoiceRetentionDays") != null ? Integer.parseInt(req.get("invoiceRetentionDays").toString()) : 2555;
        int aud = req.get("auditRetentionDays") != null ? Integer.parseInt(req.get("auditRetentionDays").toString()) : 2555;
        int sto = req.get("storageRetentionDays") != null ? Integer.parseInt(req.get("storageRetentionDays").toString()) : 2555;
        return enterpriseService.saveRetentionPolicy(TenantContext.getOrDefault(), inv, aud, sto);
    }
}
