package com.aiinvoice.enterprise.repository;

import com.aiinvoice.enterprise.entity.SsoConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SsoConfigurationRepository extends JpaRepository<SsoConfiguration, UUID> {
    Optional<SsoConfiguration> findByOrganizationId(UUID orgId);
}
