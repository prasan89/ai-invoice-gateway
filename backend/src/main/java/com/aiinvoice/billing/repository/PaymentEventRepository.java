package com.aiinvoice.billing.repository;

import com.aiinvoice.billing.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    List<PaymentEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<PaymentEvent> findByRazorpayEventId(String razorpayEventId);
}
