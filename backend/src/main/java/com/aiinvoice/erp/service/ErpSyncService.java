package com.aiinvoice.erp.service;

import com.aiinvoice.erp.connector.ErpConnector;
import com.aiinvoice.erp.entity.ErpSyncJob;
import com.aiinvoice.erp.repository.ErpSyncJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpSyncService {

    private final ErpConnector erpConnector;
    private final ErpSyncJobRepository syncJobRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public ErpSyncJob sync(UUID invoiceId, Map<String, Object> payload) {
        ErpSyncJob job = new ErpSyncJob();
        job.setId(UUID.randomUUID());
        job.setInvoiceId(invoiceId);
        job.setErpSystem(erpConnector.getSystemName());
        job.setCreatedAt(Instant.now());

        try {
            ErpConnector.ErpSyncResult result = erpConnector.push(invoiceId, payload);
            job.setSyncStatus(result.success() ? "SYNCED" : "FAILED");
            job.setResponse(writeJson(Map.of("externalRef", result.externalRef() != null ? result.externalRef() : "")));
            job.setErrorDetail(result.errorDetail());
        } catch (Exception e) {
            job.setSyncStatus("FAILED");
            job.setErrorDetail(e.getMessage());
            log.error("ERP sync failed for invoice {}: {}", invoiceId, e.getMessage());
        }

        job.setSyncedAt(Instant.now());
        return syncJobRepo.save(job);
    }

    public List<ErpSyncJob> getByInvoice(UUID invoiceId) {
        return syncJobRepo.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
    }

    private String writeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
}
