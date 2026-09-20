package com.aiinvoice.anomaly.repository;

import com.aiinvoice.anomaly.entity.InvoiceAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceAnomalyRepository extends JpaRepository<InvoiceAnomaly, UUID> {
    Optional<InvoiceAnomaly> findByInvoiceId(UUID invoiceId);
    List<InvoiceAnomaly> findByOrganizationIdOrderByDetectedAtDesc(UUID orgId);

    @Query("SELECT a FROM InvoiceAnomaly a WHERE a.organizationId = :orgId AND a.riskLevel IN ('HIGH','CRITICAL') ORDER BY a.detectedAt DESC")
    List<InvoiceAnomaly> findHighRiskByOrgId(@Param("orgId") UUID orgId);
}
