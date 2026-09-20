package com.aiinvoice.enterprise.repository;

import com.aiinvoice.enterprise.entity.ApprovalChain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ApprovalChainRepository extends JpaRepository<ApprovalChain, UUID> {
    List<ApprovalChain> findByOrganizationIdAndActiveTrue(UUID orgId);
}
