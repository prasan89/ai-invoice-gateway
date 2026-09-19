package com.aiinvoice.auth.repository;

import com.aiinvoice.auth.entity.AuthEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuthEventRepository extends JpaRepository<AuthEvent, UUID> {
}
