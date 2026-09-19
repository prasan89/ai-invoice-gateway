package com.aiinvoice.bulk.repository;

import com.aiinvoice.bulk.entity.BulkJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BulkJobRepository extends JpaRepository<BulkJob, UUID> {
    Optional<BulkJob> findByIdAndOrganizationId(UUID id, UUID orgId);
}
