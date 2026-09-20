package com.aiinvoice.erp.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ZohoBooksErpConnector implements ErpConnector {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getSystemName() { return "ZOHO_BOOKS"; }

    @Override
    public ErpSyncResult push(UUID invoiceId, Map<String, Object> payload) {
        String orgId = (String) payload.get("zoho_organization_id");
        if (orgId == null) {
            return new ErpSyncResult(false, null, "zoho_organization_id is required");
        }

        String accessToken = resolveAccessToken(payload);
        if (accessToken == null) {
            return new ErpSyncResult(false, null,
                "zoho_access_token or (zoho_client_id + zoho_client_secret + zoho_refresh_token) are required");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> bill = new LinkedHashMap<>();
            bill.put("vendor_name", payload.get("supplierName"));
            bill.put("bill_number", payload.get("invoiceNumber"));
            bill.put("date", payload.get("invoiceDate"));
            bill.put("total", payload.get("totalAmount"));
            bill.put("notes", "Imported from AI Invoice Gateway — invoice " + invoiceId);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("bill", bill), headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                "https://www.zohoapis.in/books/v3/bills?organization_id=" + orgId,
                HttpMethod.POST, req, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<?, ?> billResp = (Map<?, ?>) resp.getBody().get("bill");
                String externalRef = billResp != null ? (String) billResp.get("bill_id") : null;
                return new ErpSyncResult(true, externalRef, null);
            }
            return new ErpSyncResult(false, null, "Zoho returned " + resp.getStatusCode());
        } catch (Exception e) {
            log.warn("Zoho Books sync failed for invoice {}: {}", invoiceId, e.getMessage());
            return new ErpSyncResult(false, null, e.getMessage());
        }
    }

    private String resolveAccessToken(Map<String, Object> payload) {
        String accessToken = (String) payload.get("zoho_access_token");
        if (accessToken != null && !accessToken.isBlank()) return accessToken;

        // Attempt token refresh using stored credentials
        String clientId = (String) payload.get("zoho_client_id");
        String clientSecret = (String) payload.get("zoho_client_secret");
        String refreshToken = (String) payload.get("zoho_refresh_token");
        if (clientId == null || clientSecret == null || refreshToken == null) return null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                "https://accounts.zoho.in/oauth/v2/token",
                HttpMethod.POST, req, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return (String) resp.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.warn("Zoho token refresh failed: {}", e.getMessage());
        }
        return null;
    }
}
