package com.aiinvoice.erp.repository;

import com.aiinvoice.erp.entity.ErpConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ErpConnectionRepository extends JpaRepository<ErpConnection, UUID> {
    List<ErpConnection> findByOrganizationId(UUID orgId);
    Optional<ErpConnection> findByOrganizationIdAndErpSystem(UUID orgId, String erpSystem);
}
