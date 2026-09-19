package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.GstinVerification;
import com.aiinvoice.invoice.repository.GstinVerificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Verifies a GSTIN against the GST portal sandbox API.
 *
 * Two modes:
 *  - "sandbox": hits https://api.gst.gov.in/commonapi/v1.1/search/taxpayerDetails
 *    (public, no auth key required for basic taxpayer search in sandbox)
 *  - "offline": skips external call, returns UNKNOWN status immediately
 *
 * Results are cached in gstin_verifications for CACHE_HOURS to avoid
 * hammering the portal on every invoice upload.
 */
@Service
public class GstinPortalService {

    private static final int CACHE_HOURS = 24;
    private static final String SANDBOX_URL =
        "https://api.gst.gov.in/commonapi/v1.1/search/taxpayerDetails?gstin=";

    public record GstinPortalResult(
        String gstin,
        String legalName,
        String tradeName,
        String registrationStatus,  // ACTIVE, CANCELLED, SUSPENDED, UNKNOWN
        String stateCode,
        boolean fromCache
    ) {
        public boolean isActive() { return "ACTIVE".equals(registrationStatus); }
        public boolean isUnknown() { return "UNKNOWN".equals(registrationStatus); }
    }

    private final GstinVerificationRepository repo;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final boolean enabled;

    public GstinPortalService(
            GstinVerificationRepository repo,
            ObjectMapper mapper,
            @Value("${gstin.portal.enabled:false}") boolean enabled) {
        this.repo = repo;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.enabled = enabled;
    }

    @Transactional
    public GstinPortalResult verify(String gstin) {
        if (gstin == null || gstin.isBlank()) {
            return offline(gstin);
        }
        String g = gstin.trim().toUpperCase();

        // Serve from cache if fresh
        Instant freshCutoff = Instant.now().minus(CACHE_HOURS, ChronoUnit.HOURS);
        var cached = repo.findFresh(g, freshCutoff);
        if (cached.isPresent()) {
            return fromEntity(cached.get(), true);
        }

        if (!enabled) {
            return offline(g);
        }

        return callPortal(g);
    }

    private GstinPortalResult callPortal(String gstin) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SANDBOX_URL + gstin))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseAndCache(gstin, response.body());
            }
            // Portal returned non-200 (GSTIN not found, etc.)
            return cacheAndReturn(gstin, null, null, "UNKNOWN", null, response.body());
        } catch (Exception e) {
            // Network failure — don't fail the invoice upload, just return UNKNOWN
            return offline(gstin);
        }
    }

    private GstinPortalResult parseAndCache(String gstin, String body) {
        try {
            JsonNode root = mapper.readTree(body);
            // GST portal response structure: { "data": { "lgnm": ..., "tradeNam": ..., "sts": ..., "stj": ... } }
            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                return cacheAndReturn(gstin, null, null, "UNKNOWN", null, body);
            }

            String legalName = data.path("lgnm").asText(null);
            String tradeName = data.path("tradeNam").asText(null);
            String status    = mapStatus(data.path("sts").asText(null));
            // State code is first 2 chars of GSTIN
            String stateCode = gstin.length() >= 2 ? gstin.substring(0, 2) : null;

            return cacheAndReturn(gstin, legalName, tradeName, status, stateCode, body);
        } catch (Exception e) {
            return cacheAndReturn(gstin, null, null, "UNKNOWN", null, body);
        }
    }

    private GstinPortalResult cacheAndReturn(String gstin, String legalName, String tradeName,
                                              String status, String stateCode, String rawResponse) {
        GstinVerification v = repo.findById(gstin).orElse(new GstinVerification());
        v.setGstin(gstin);
        v.setLegalName(legalName);
        v.setTradeName(tradeName);
        v.setRegistrationStatus(status);
        v.setStateCode(stateCode);
        v.setVerifiedAt(Instant.now());
        try {
            v.setRawResponse(rawResponse != null ? rawResponse : "{}");
        } catch (Exception ignored) {}
        repo.save(v);
        return new GstinPortalResult(gstin, legalName, tradeName, status, stateCode, false);
    }

    private GstinPortalResult fromEntity(GstinVerification v, boolean fromCache) {
        return new GstinPortalResult(v.getGstin(), v.getLegalName(), v.getTradeName(),
            v.getRegistrationStatus(), v.getStateCode(), fromCache);
    }

    private GstinPortalResult offline(String gstin) {
        return new GstinPortalResult(gstin, null, null, "UNKNOWN", null, false);
    }

    private String mapStatus(String raw) {
        if (raw == null) return "UNKNOWN";
        return switch (raw.trim().toUpperCase()) {
            case "ACTIVE"    -> "ACTIVE";
            case "CANCELLED" -> "CANCELLED";
            case "SUSPENDED" -> "SUSPENDED";
            default          -> "UNKNOWN";
        };
    }
}
