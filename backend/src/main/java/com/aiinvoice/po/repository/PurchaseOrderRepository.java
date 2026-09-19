package com.aiinvoice.po.repository;

import com.aiinvoice.po.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    List<PurchaseOrder> findByOrganizationIdOrderByCreatedAtDesc(UUID orgId);

    Optional<PurchaseOrder> findByOrganizationIdAndPoNumber(UUID orgId, String poNumber);

    @Query("SELECT p FROM PurchaseOrder p LEFT JOIN FETCH p.lines WHERE p.id = :id")
    Optional<PurchaseOrder> findByIdWithLines(UUID id);

    @Query("SELECT p FROM PurchaseOrder p LEFT JOIN FETCH p.lines WHERE p.organizationId = :orgId AND p.supplierGstin = :gstin")
    List<PurchaseOrder> findByOrgAndSupplier(UUID orgId, String gstin);
}
