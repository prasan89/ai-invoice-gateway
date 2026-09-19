package com.aiinvoice.auth.filter;

import com.aiinvoice.auth.context.PrincipalContext;
import com.aiinvoice.auth.context.TenantContext;
import com.aiinvoice.auth.entity.User;
import com.aiinvoice.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(5)
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = req.getHeader("X-Session-Token");
        if (token != null) {
            try {
                Optional<User> user = authService.validateSession(UUID.fromString(token));
                user.ifPresent(u -> {
                    PrincipalContext.set(u);
                    TenantContext.set(u.getOrganizationId());
                });
            } catch (IllegalArgumentException ignored) {}
        }
        try {
            chain.doFilter(req, res);
        } finally {
            PrincipalContext.clear();
            TenantContext.clear();
        }
    }
}
