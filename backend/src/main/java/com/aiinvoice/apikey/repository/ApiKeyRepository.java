package com.aiinvoice.apikey.repository;

import com.aiinvoice.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    @Query("SELECT k FROM ApiKey k WHERE k.keyHash = :hash AND k.revokedAt IS NULL")
    Optional<ApiKey> findActiveByKeyHash(@Param("hash") String hash);
    List<ApiKey> findByOrganizationIdAndRevokedAtIsNull(UUID organizationId);
}
