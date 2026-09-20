package com.aiinvoice.billing.repository;

import com.aiinvoice.billing.entity.OrganizationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, UUID> {
    Optional<OrganizationSubscription> findByOrganizationId(UUID organizationId);
}
