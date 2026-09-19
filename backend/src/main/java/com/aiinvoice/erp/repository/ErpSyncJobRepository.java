package com.aiinvoice.erp.repository;

import com.aiinvoice.erp.entity.ErpSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ErpSyncJobRepository extends JpaRepository<ErpSyncJob, UUID> {
    List<ErpSyncJob> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
