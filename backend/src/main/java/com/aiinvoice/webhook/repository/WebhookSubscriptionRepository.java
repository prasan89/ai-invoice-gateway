package com.aiinvoice.webhook.repository;

import com.aiinvoice.webhook.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findByOrganizationIdAndActiveTrue(UUID organizationId);
    List<WebhookSubscription> findByActiveTrue();
}
