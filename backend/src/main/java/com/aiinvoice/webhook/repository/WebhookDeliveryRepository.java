package com.aiinvoice.webhook.repository;

import com.aiinvoice.webhook.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
}
