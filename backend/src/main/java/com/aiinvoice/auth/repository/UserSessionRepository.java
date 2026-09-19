package com.aiinvoice.auth.repository;

import com.aiinvoice.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findBySessionTokenAndExpiresAtAfter(UUID sessionToken, Instant now);
    void deleteBySessionToken(UUID sessionToken);
}
