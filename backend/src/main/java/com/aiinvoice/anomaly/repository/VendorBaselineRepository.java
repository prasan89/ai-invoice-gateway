package com.aiinvoice.anomaly.repository;

import com.aiinvoice.anomaly.entity.VendorBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VendorBaselineRepository extends JpaRepository<VendorBaseline, UUID> {
    Optional<VendorBaseline> findByOrganizationIdAndSupplierGstin(UUID orgId, String gstin);
}
