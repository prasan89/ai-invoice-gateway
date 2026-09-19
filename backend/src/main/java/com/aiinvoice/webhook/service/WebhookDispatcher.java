package com.aiinvoice.webhook.service;

import com.aiinvoice.webhook.entity.WebhookDelivery;
import com.aiinvoice.webhook.entity.WebhookSubscription;
import com.aiinvoice.webhook.repository.WebhookDeliveryRepository;
import com.aiinvoice.webhook.repository.WebhookSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcher {

    private final WebhookSubscriptionRepository subRepo;
    private final WebhookDeliveryRepository deliveryRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Async
    public void dispatch(UUID organizationId, String eventType, Map<String, Object> payload) {
        List<WebhookSubscription> subs = subRepo.findByOrganizationIdAndActiveTrue(organizationId);
        for (WebhookSubscription sub : subs) {
            if (!isSubscribed(sub, eventType)) continue;
            try {
                String body = objectMapper.writeValueAsString(Map.of(
                        "event", eventType,
                        "timestamp", Instant.now().toString(),
                        "data", payload
                ));
                String sig = hmacSha256(sub.getSecret(), body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getUrl()))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")
                        .header("X-Webhook-Signature", "sha256=" + sig)
                        .timeout(Duration.ofSeconds(10))
                        .build();

                boolean success = false;
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) { success = true; break; }
                    } catch (Exception e) {
                        if (attempt == 3) log.warn("Webhook delivery failed after 3 attempts: {}", sub.getUrl());
                    }
                }

                WebhookDelivery delivery = new WebhookDelivery();
                delivery.setId(UUID.randomUUID());
                delivery.setSubscriptionId(sub.getId());
                delivery.setEventType(eventType);
                delivery.setPayload(body);
                delivery.setStatus(success ? "DELIVERED" : "FAILED");
                delivery.setAttempts(3);
                delivery.setLastAttemptAt(Instant.now());
                delivery.setCreatedAt(Instant.now());
                deliveryRepo.save(delivery);

            } catch (Exception e) {
                log.error("Webhook dispatch error: {}", e.getMessage());
            }
        }
    }

    private boolean isSubscribed(WebhookSubscription sub, String eventType) {
        String[] events = sub.getEvents();
        return events != null && (Arrays.asList(events).contains("*") || Arrays.asList(events).contains(eventType));
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
