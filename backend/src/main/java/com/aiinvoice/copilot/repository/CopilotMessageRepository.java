package com.aiinvoice.copilot.repository;

import com.aiinvoice.copilot.entity.CopilotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CopilotMessageRepository extends JpaRepository<CopilotMessage, UUID> {
    List<CopilotMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
