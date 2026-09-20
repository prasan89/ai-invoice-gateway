package com.aiinvoice.enterprise.repository;

import com.aiinvoice.enterprise.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    List<ApprovalRequest> findByOrganizationIdAndStatus(UUID orgId, String status);
    Optional<ApprovalRequest> findByInvoiceId(UUID invoiceId);
}
