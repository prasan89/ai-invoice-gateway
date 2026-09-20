package com.aiinvoice.enterprise.service;

import com.aiinvoice.enterprise.entity.*;
import com.aiinvoice.enterprise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final SsoConfigurationRepository ssoRepo;
    private final ApprovalChainRepository chainRepo;
    private final ApprovalRequestRepository requestRepo;
    private final DataRetentionPolicyRepository retentionRepo;

    // SSO

    public Optional<SsoConfiguration> getSsoConfig(UUID orgId) {
        return ssoRepo.findByOrganizationId(orgId);
    }

    @Transactional
    public SsoConfiguration saveSsoConfig(UUID orgId, String provider, String entityId,
                                          String metadataUrl, String clientId, String clientSecret,
                                          String issuer, boolean enabled) {
        SsoConfiguration cfg = ssoRepo.findByOrganizationId(orgId).orElseGet(() -> {
            SsoConfiguration c = new SsoConfiguration();
            c.setId(UUID.randomUUID());
            c.setOrganizationId(orgId);
            c.setCreatedAt(Instant.now());
            return c;
        });
        cfg.setProvider(provider);
        cfg.setEntityId(entityId);
        cfg.setMetadataUrl(metadataUrl);
        cfg.setClientId(clientId);
        if (clientSecret != null && !clientSecret.isBlank()) cfg.setClientSecret(clientSecret);
        cfg.setIssuer(issuer);
        cfg.setEnabled(enabled);
        cfg.setUpdatedAt(Instant.now());
        return ssoRepo.save(cfg);
    }

    // Approval chains

    public List<ApprovalChain> listChains(UUID orgId) {
        return chainRepo.findByOrganizationIdAndActiveTrue(orgId);
    }

    @Transactional
    public ApprovalChain saveChain(UUID orgId, UUID chainId, String name, String description,
                                   BigDecimal minAmount, BigDecimal maxAmount, String stepsJson) {
        ApprovalChain chain = chainId != null
            ? chainRepo.findById(chainId).orElseGet(ApprovalChain::new)
            : new ApprovalChain();
        if (chain.getId() == null) chain.setId(UUID.randomUUID());
        chain.setOrganizationId(orgId);
        chain.setName(name);
        chain.setDescription(description);
        chain.setMinAmount(minAmount);
        chain.setMaxAmount(maxAmount);
        chain.setSteps(stepsJson);
        chain.setActive(true);
        chain.setUpdatedAt(Instant.now());
        if (chain.getCreatedAt() == null) chain.setCreatedAt(Instant.now());
        return chainRepo.save(chain);
    }

    @Transactional
    public ApprovalRequest initiateApproval(UUID orgId, UUID invoiceId, UUID chainId) {
        ApprovalRequest req = requestRepo.findByInvoiceId(invoiceId).orElseGet(() -> {
            ApprovalRequest r = new ApprovalRequest();
            r.setId(UUID.randomUUID());
            r.setCreatedAt(Instant.now());
            return r;
        });
        req.setOrganizationId(orgId);
        req.setInvoiceId(invoiceId);
        req.setChainId(chainId);
        req.setCurrentStep(0);
        req.setStatus("PENDING");
        req.setStepsState("[]");
        req.setUpdatedAt(Instant.now());
        return requestRepo.save(req);
    }

    @Transactional
    public ApprovalRequest advanceApproval(UUID requestId, String outcome, UUID actorId) {
        ApprovalRequest req = requestRepo.findById(requestId)
            .orElseThrow(() -> new NoSuchElementException("Approval request not found"));
        if ("APPROVED".equals(outcome)) {
            req.setCurrentStep(req.getCurrentStep() + 1);
            ApprovalChain chain = chainRepo.findById(req.getChainId()).orElse(null);
            int totalSteps = 1;
            if (chain != null && chain.getSteps() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
                    totalSteps = m.readValue(chain.getSteps(), List.class).size();
                } catch (Exception ignored) {}
            }
            if (req.getCurrentStep() >= totalSteps) req.setStatus("APPROVED");
        } else {
            req.setStatus("REJECTED");
        }
        req.setUpdatedAt(Instant.now());
        return requestRepo.save(req);
    }

    public List<ApprovalRequest> listPendingApprovals(UUID orgId) {
        return requestRepo.findByOrganizationIdAndStatus(orgId, "PENDING");
    }

    // Data retention

    public DataRetentionPolicy getRetentionPolicy(UUID orgId) {
        return retentionRepo.findByOrganizationId(orgId).orElseGet(() -> {
            DataRetentionPolicy p = new DataRetentionPolicy();
            p.setId(UUID.randomUUID());
            p.setOrganizationId(orgId);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return retentionRepo.save(p);
        });
    }

    @Transactional
    public DataRetentionPolicy saveRetentionPolicy(UUID orgId, int invoiceDays, int auditDays, int storageDays) {
        DataRetentionPolicy p = getRetentionPolicy(orgId);
        p.setInvoiceRetentionDays(invoiceDays);
        p.setAuditRetentionDays(auditDays);
        p.setStorageRetentionDays(storageDays);
        p.setUpdatedAt(Instant.now());
        return retentionRepo.save(p);
    }
}
