package com.aiinvoice.copilot.repository;

import com.aiinvoice.copilot.entity.CopilotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CopilotSessionRepository extends JpaRepository<CopilotSession, UUID> {
    List<CopilotSession> findByOrganizationIdOrderByUpdatedAtDesc(UUID orgId);
}
