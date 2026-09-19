package com.aiinvoice.webhook.service;

import com.aiinvoice.webhook.dto.CreateWebhookRequest;
import com.aiinvoice.webhook.dto.WebhookDto;
import com.aiinvoice.webhook.entity.WebhookSubscription;
import com.aiinvoice.webhook.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookSubscriptionRepository subRepo;

    public List<WebhookDto> listByOrg(UUID organizationId) {
        return subRepo.findByOrganizationIdAndActiveTrue(organizationId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public WebhookDto create(UUID organizationId, CreateWebhookRequest req) {
        WebhookSubscription sub = new WebhookSubscription();
        sub.setId(UUID.randomUUID());
        sub.setOrganizationId(organizationId);
        sub.setUrl(req.url());
        sub.setEvents(req.events().toArray(String[]::new));
        sub.setSecret(UUID.randomUUID().toString().replace("-", ""));
        sub.setActive(true);
        sub.setCreatedAt(Instant.now());
        return toDto(subRepo.save(sub));
    }

    @Transactional
    public void delete(UUID id) {
        subRepo.findById(id).ifPresent(s -> {
            s.setActive(false);
            subRepo.save(s);
        });
    }

    private WebhookDto toDto(WebhookSubscription s) {
        List<String> events = s.getEvents() != null ? Arrays.asList(s.getEvents()) : List.of();
        return new WebhookDto(s.getId(), s.getUrl(), events, s.isActive(), s.getCreatedAt());
    }
}
