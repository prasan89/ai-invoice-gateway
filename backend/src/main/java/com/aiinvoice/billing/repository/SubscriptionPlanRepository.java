package com.aiinvoice.billing.repository;

import com.aiinvoice.billing.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    List<SubscriptionPlan> findByActiveTrueOrderByMonthlyPricePaiseAsc();
    Optional<SubscriptionPlan> findByName(String name);
}
