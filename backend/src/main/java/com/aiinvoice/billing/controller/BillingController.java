package com.aiinvoice.billing.controller;

import com.aiinvoice.auth.context.TenantContext;
import com.aiinvoice.billing.dto.*;
import com.aiinvoice.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    public List<PlanDto> listPlans() {
        return billingService.listPlans();
    }

    @GetMapping("/subscription")
    public SubscriptionDto getSubscription() {
        return billingService.getSubscription(TenantContext.get());
    }

    @PostMapping("/subscription/plan")
    public SubscriptionDto changePlan(@RequestBody ChangePlanRequest req) {
        return billingService.changePlan(TenantContext.get(), req.planName());
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<Void> cancel() {
        billingService.cancelSubscription(TenantContext.get());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usage")
    public List<UsageDto> usageHistory() {
        return billingService.getUsageHistory(TenantContext.get());
    }
}
