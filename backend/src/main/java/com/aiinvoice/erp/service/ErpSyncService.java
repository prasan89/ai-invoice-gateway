package com.aiinvoice.erp.service;

import com.aiinvoice.erp.connector.ErpConnector;
import com.aiinvoice.erp.entity.ErpConnection;
import com.aiinvoice.erp.entity.ErpSyncJob;
import com.aiinvoice.erp.repository.ErpConnectionRepository;
import com.aiinvoice.erp.repository.ErpSyncJobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class ErpSyncService {

    private final Map<String, ErpConnector> connectors;
    private final ErpSyncJobRepository syncJobRepo;
    private final ErpConnectionRepository connectionRepo;
    private final ObjectMapper objectMapper;

    public ErpSyncService(List<ErpConnector> connectorList,
                          ErpSyncJobRepository syncJobRepo,
                          ErpConnectionRepository connectionRepo,
                          ObjectMapper objectMapper) {
        this.syncJobRepo = syncJobRepo;
        this.connectionRepo = connectionRepo;
        this.objectMapper = objectMapper;
        this.connectors = new HashMap<>();
        connectorList.forEach(c -> connectors.put(c.getSystemName(), c));
    }

    @Transactional
    public ErpSyncJob sync(UUID orgId, UUID invoiceId, Map<String, Object> payload) {
        String erpSystem = resolveErpSystem(orgId, payload);
        ErpConnector connector = connectors.getOrDefault(erpSystem, connectors.get("mock"));
        if (connector == null) connector = connectors.values().iterator().next();

        ErpSyncJob job = new ErpSyncJob();
        job.setId(UUID.randomUUID());
        job.setInvoiceId(invoiceId);
        job.setErpSystem(erpSystem);
        job.setCreatedAt(Instant.now());

        // Merge connection config into payload
        connectionRepo.findByOrganizationIdAndErpSystem(orgId, erpSystem).ifPresent(conn -> {
            try {
                Map<String, Object> config = objectMapper.readValue(conn.getConfig(), new TypeReference<>() {});
                config.forEach(payload::putIfAbsent);
            } catch (Exception e) { /* ignore parse errors */ }
        });

        try {
            ErpConnector.ErpSyncResult result = connector.push(invoiceId, payload);
            job.setSyncStatus(result.success() ? "SYNCED" : "FAILED");
            job.setResponse(writeJson(Map.of("externalRef", result.externalRef() != null ? result.externalRef() : "")));
            job.setErrorDetail(result.errorDetail());

            if (result.success()) {
                connectionRepo.findByOrganizationIdAndErpSystem(orgId, erpSystem).ifPresent(conn -> {
                    conn.setLastSyncedAt(Instant.now());
                    conn.setStatus("CONNECTED");
                    conn.setUpdatedAt(Instant.now());
                    connectionRepo.save(conn);
                });
            }
        } catch (Exception e) {
            job.setSyncStatus("FAILED");
            job.setErrorDetail(e.getMessage());
            log.error("ERP sync failed for invoice {}: {}", invoiceId, e.getMessage());
        }

        job.setSyncedAt(Instant.now());
        return syncJobRepo.save(job);
    }

    @Transactional
    public ErpConnection saveConnection(UUID orgId, String erpSystem, String displayName, Map<String, Object> config) {
        ErpConnection conn = connectionRepo.findByOrganizationIdAndErpSystem(orgId, erpSystem)
            .orElseGet(() -> {
                ErpConnection c = new ErpConnection();
                c.setId(UUID.randomUUID());
                c.setOrganizationId(orgId);
                c.setCreatedAt(Instant.now());
                return c;
            });
        conn.setErpSystem(erpSystem);
        conn.setDisplayName(displayName);
        conn.setConfig(writeJson(config));
        conn.setStatus("CONFIGURED");
        conn.setUpdatedAt(Instant.now());
        return connectionRepo.save(conn);
    }

    public List<ErpConnection> listConnections(UUID orgId) {
        return connectionRepo.findByOrganizationId(orgId);
    }

    public List<ErpSyncJob> getByInvoice(UUID invoiceId) {
        return syncJobRepo.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
    }

    private String resolveErpSystem(UUID orgId, Map<String, Object> payload) {
        if (payload.containsKey("erpSystem")) return (String) payload.get("erpSystem");
        return connectionRepo.findByOrganizationId(orgId).stream()
            .filter(c -> "CONNECTED".equals(c.getStatus()) || "CONFIGURED".equals(c.getStatus()))
            .map(ErpConnection::getErpSystem)
            .findFirst()
            .orElse("MOCK");
    }

    private String writeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
}
