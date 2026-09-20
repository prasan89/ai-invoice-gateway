package com.aiinvoice.enterprise.repository;

import com.aiinvoice.enterprise.entity.DataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, UUID> {
    Optional<DataRetentionPolicy> findByOrganizationId(UUID orgId);
}
