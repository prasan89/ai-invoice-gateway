package com.aiinvoice.billing.service;

import com.aiinvoice.billing.dto.*;
import com.aiinvoice.billing.entity.*;
import com.aiinvoice.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final SubscriptionPlanRepository planRepo;
    private final OrganizationSubscriptionRepository subRepo;
    private final PaymentEventRepository paymentRepo;
    private final UsageRecordRepository usageRepo;

    public List<PlanDto> listPlans() {
        return planRepo.findByActiveTrueOrderByMonthlyPricePaiseAsc().stream()
            .map(this::toPlanDto).toList();
    }

    public SubscriptionDto getSubscription(UUID orgId) {
        OrganizationSubscription sub = subRepo.findByOrganizationId(orgId)
            .orElseGet(() -> createTrialSubscription(orgId));
        return toDto(sub);
    }

    @Transactional
    public SubscriptionDto changePlan(UUID orgId, String planName) {
        SubscriptionPlan plan = planRepo.findByName(planName)
            .orElseThrow(() -> new IllegalArgumentException("Unknown plan: " + planName));
        OrganizationSubscription sub = subRepo.findByOrganizationId(orgId)
            .orElseGet(() -> createTrialSubscription(orgId));
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 24 * 3600));
        sub.setUpdatedAt(Instant.now());
        return toDto(subRepo.save(sub));
    }

    @Transactional
    public void cancelSubscription(UUID orgId) {
        subRepo.findByOrganizationId(orgId).ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            sub.setCancelledAt(Instant.now());
            sub.setUpdatedAt(Instant.now());
            subRepo.save(sub);
        });
    }

    @Transactional
    public void incrementInvoiceUsage(UUID orgId) {
        OrganizationSubscription sub = subRepo.findByOrganizationId(orgId).orElse(null);
        if (sub == null) return;
        sub.setInvoiceCountCurrent(sub.getInvoiceCountCurrent() + 1);
        sub.setUpdatedAt(Instant.now());
        subRepo.save(sub);

        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        UsageRecord usage = usageRepo.findByOrganizationIdAndPeriodStart(orgId, periodStart)
            .orElseGet(() -> {
                UsageRecord r = new UsageRecord();
                r.setId(UUID.randomUUID());
                r.setOrganizationId(orgId);
                r.setPeriodStart(periodStart);
                r.setPeriodEnd(periodStart.withDayOfMonth(periodStart.lengthOfMonth()));
                r.setCreatedAt(Instant.now());
                return r;
            });
        usage.setInvoiceCount(usage.getInvoiceCount() + 1);
        usage.setUpdatedAt(Instant.now());
        usageRepo.save(usage);
    }

    public boolean isWithinLimit(UUID orgId) {
        OrganizationSubscription sub = subRepo.findByOrganizationId(orgId).orElse(null);
        if (sub == null) return false;
        int limit = sub.getPlan().getInvoiceLimit();
        return limit == -1 || sub.getInvoiceCountCurrent() < limit;
    }

    public List<UsageDto> getUsageHistory(UUID orgId) {
        return usageRepo.findRecentByOrganizationId(orgId, PageRequest.of(0, 12))
            .stream().map(this::toUsageDto).toList();
    }

    // Called by Razorpay webhook handler
    @Transactional
    public void handleWebhookEvent(String razorpayEventId, String eventType,
                                   UUID orgId, Long amountPaise, String payload) {
        if (paymentRepo.findByRazorpayEventId(razorpayEventId).isPresent()) return; // idempotent
        PaymentEvent ev = new PaymentEvent();
        ev.setId(UUID.randomUUID());
        ev.setOrganizationId(orgId);
        ev.setRazorpayEventId(razorpayEventId);
        ev.setEventType(eventType);
        ev.setAmountPaise(amountPaise);
        ev.setPayload(payload);
        ev.setCreatedAt(Instant.now());
        paymentRepo.save(ev);

        if ("subscription.charged".equals(eventType)) {
            subRepo.findByOrganizationId(orgId).ifPresent(sub -> {
                sub.setStatus("ACTIVE");
                sub.setInvoiceCountCurrent(0); // reset on new billing period
                sub.setCurrentPeriodStart(Instant.now());
                sub.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 24 * 3600));
                sub.setUpdatedAt(Instant.now());
                subRepo.save(sub);
            });
        } else if ("subscription.cancelled".equals(eventType) || "payment.failed".equals(eventType)) {
            subRepo.findByOrganizationId(orgId).ifPresent(sub -> {
                sub.setStatus("past_due".equals(eventType) ? "PAST_DUE" : "CANCELLED");
                sub.setUpdatedAt(Instant.now());
                subRepo.save(sub);
            });
        }
    }

    private OrganizationSubscription createTrialSubscription(UUID orgId) {
        SubscriptionPlan starter = planRepo.findByName("STARTER")
            .orElseThrow(() -> new IllegalStateException("STARTER plan not seeded"));
        OrganizationSubscription sub = new OrganizationSubscription();
        sub.setId(UUID.randomUUID());
        sub.setOrganizationId(orgId);
        sub.setPlan(starter);
        sub.setStatus("TRIALING");
        sub.setTrialEnd(Instant.now().plusSeconds(14L * 24 * 3600));
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 24 * 3600));
        sub.setCreatedAt(Instant.now());
        sub.setUpdatedAt(Instant.now());
        return subRepo.save(sub);
    }

    private SubscriptionDto toDto(OrganizationSubscription sub) {
        SubscriptionPlan p = sub.getPlan();
        int limit = p.getInvoiceLimit();
        int remaining = limit == -1 ? Integer.MAX_VALUE : Math.max(0, limit - sub.getInvoiceCountCurrent());
        return new SubscriptionDto(
            sub.getId(), p.getName(), p.getDisplayName(),
            p.getMonthlyPricePaise(), p.getInvoiceLimit(),
            p.getApiKeyLimit(), p.getUserLimit(),
            featuresFromJson(p.getFeatures()),
            sub.getStatus(), sub.getCurrentPeriodStart(),
            sub.getCurrentPeriodEnd(), sub.getTrialEnd(),
            sub.getInvoiceCountCurrent(), remaining
        );
    }

    private PlanDto toPlanDto(SubscriptionPlan p) {
        return new PlanDto(p.getId(), p.getName(), p.getDisplayName(),
            p.getMonthlyPricePaise(), p.getInvoiceLimit(),
            p.getApiKeyLimit(), p.getUserLimit(), featuresFromJson(p.getFeatures()));
    }

    private UsageDto toUsageDto(UsageRecord r) {
        return new UsageDto(r.getId(), r.getPeriodStart(), r.getPeriodEnd(),
            r.getInvoiceCount(), r.getApiCalls(), r.getAiExtractions());
    }

    @SuppressWarnings("unchecked")
    private List<String> featuresFromJson(String json) {
        if (json == null) return List.of();
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            return m.readValue(json, List.class);
        } catch (Exception e) { return List.of(); }
    }
}
