package com.aiinvoice.auth.service;

import com.aiinvoice.auth.dto.LoginRequest;
import com.aiinvoice.auth.dto.LoginResponse;
import com.aiinvoice.auth.dto.RegisterRequest;
import com.aiinvoice.auth.entity.User;
import com.aiinvoice.auth.entity.UserSession;
import com.aiinvoice.auth.repository.UserRepository;
import com.aiinvoice.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final UserSessionRepository sessionRepo;
    private final BCryptPasswordEncoder bcrypt;
    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (userRepo.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganizationId(DEMO_ORG);
        user.setEmail(req.email());
        user.setPasswordHash(bcrypt.encode(req.password()));
        user.setRole(req.role() != null ? req.role() : "ANALYST");
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);
        return createSession(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!bcrypt.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return createSession(user);
    }

    @Transactional
    public void logout(UUID sessionToken) {
        sessionRepo.deleteBySessionToken(sessionToken);
    }

    public Optional<User> validateSession(UUID sessionToken) {
        return sessionRepo.findBySessionTokenAndExpiresAtAfter(sessionToken, Instant.now())
                .flatMap(s -> userRepo.findById(s.getUserId()));
    }

    private LoginResponse createSession(User user) {
        UserSession session = new UserSession();
        session.setSessionToken(UUID.randomUUID());
        session.setUserId(user.getId());
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        sessionRepo.save(session);
        return new LoginResponse(session.getSessionToken().toString(), user.getId(),
                user.getEmail(), user.getRole(), user.getOrganizationId());
    }
}
