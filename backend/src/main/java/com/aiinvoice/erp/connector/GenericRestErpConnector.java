package com.aiinvoice.erp.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generic REST connector — posts the invoice payload to any user-configured REST endpoint.
 * Useful for custom ERP systems or middleware.
 */
@Component
@Slf4j
public class GenericRestErpConnector implements ErpConnector {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getSystemName() { return "GENERIC_REST"; }

    @Override
    public ErpSyncResult push(UUID invoiceId, Map<String, Object> payload) {
        String endpoint = (String) payload.get("rest_endpoint");
        if (endpoint == null) return new ErpSyncResult(false, null, "rest_endpoint is required in config");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = (String) payload.get("rest_auth_header");
            if (authHeader != null) headers.set("Authorization", authHeader);

            Map<String, Object> body = new LinkedHashMap<>(payload);
            body.remove("rest_endpoint");
            body.remove("rest_auth_header");
            body.put("source", "ai-invoice-gateway");
            body.put("invoiceId", invoiceId.toString());

            ResponseEntity<Map> resp = restTemplate.exchange(
                endpoint, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                String ref = resp.getBody() != null ? resp.getBody().getOrDefault("id", invoiceId).toString() : invoiceId.toString();
                return new ErpSyncResult(true, ref, null);
            }
            return new ErpSyncResult(false, null, "HTTP " + resp.getStatusCode());
        } catch (Exception e) {
            log.warn("Generic REST ERP sync failed for invoice {}: {}", invoiceId, e.getMessage());
            return new ErpSyncResult(false, null, e.getMessage());
        }
    }
}
