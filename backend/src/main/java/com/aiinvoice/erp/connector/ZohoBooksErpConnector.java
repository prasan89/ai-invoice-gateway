package com.aiinvoice.erp.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zoho Books connector via Zoho Books REST API v3.
 * Requires OAuth2 access token (refreshed via client_credentials or refresh_token).
 */
@Component
@Slf4j
public class ZohoBooksErpConnector implements ErpConnector {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getSystemName() { return "ZOHO_BOOKS"; }

    @Override
    public ErpSyncResult push(UUID invoiceId, Map<String, Object> payload) {
        String accessToken = (String) payload.get("zoho_access_token");
        String orgId = (String) payload.get("zoho_organization_id");
        if (accessToken == null || orgId == null) {
            return new ErpSyncResult(false, null, "zoho_access_token and zoho_organization_id are required");
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
}
