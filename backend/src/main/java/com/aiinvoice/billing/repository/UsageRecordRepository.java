package com.aiinvoice.billing.repository;

import com.aiinvoice.billing.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    Optional<UsageRecord> findByOrganizationIdAndPeriodStart(UUID orgId, LocalDate periodStart);

    @Query("SELECT u FROM UsageRecord u WHERE u.organizationId = :orgId ORDER BY u.periodStart DESC")
    List<UsageRecord> findRecentByOrganizationId(@Param("orgId") UUID orgId,
        org.springframework.data.domain.Pageable pageable);
}
