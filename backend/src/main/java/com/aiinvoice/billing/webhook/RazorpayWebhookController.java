package com.aiinvoice.billing.webhook;

import com.aiinvoice.billing.service.BillingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/razorpay/webhook")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final BillingService billingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (!webhookSecret.isBlank()) {
            if (!verifySignature(rawBody, signature)) {
                log.warn("Razorpay webhook signature mismatch");
                return ResponseEntity.status(400).build();
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("event").asText();
            String razorpayEventId = root.path("id").asText(UUID.randomUUID().toString());

            // Extract organization from subscription notes or customer external_id
            String orgIdStr = root.at("/payload/subscription/entity/notes/organization_id")
                .asText(null);
            if (orgIdStr == null) orgIdStr = root.at("/payload/payment/entity/notes/organization_id")
                .asText(null);

            UUID orgId = orgIdStr != null ? UUID.fromString(orgIdStr) : null;
            if (orgId == null) {
                log.info("Razorpay webhook {} has no organization_id — skipping", eventType);
                return ResponseEntity.ok().build();
            }

            long amountPaise = root.at("/payload/payment/entity/amount").asLong(0);
            billingService.handleWebhookEvent(razorpayEventId, eventType, orgId, amountPaise, rawBody);

        } catch (Exception e) {
            log.error("Razorpay webhook processing error", e);
        }

        return ResponseEntity.ok().build();
    }

    private boolean verifySignature(String body, String signature) {
        if (signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(computed).equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
