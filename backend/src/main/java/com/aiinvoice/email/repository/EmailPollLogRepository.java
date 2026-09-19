package com.aiinvoice.email.repository;

import com.aiinvoice.email.entity.EmailPollLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailPollLogRepository extends JpaRepository<EmailPollLog, UUID> {
    List<EmailPollLog> findTop20ByOrganizationIdOrderByPolledAtDesc(UUID orgId);
}
