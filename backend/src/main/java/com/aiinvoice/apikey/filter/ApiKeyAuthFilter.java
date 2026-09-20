package com.aiinvoice.apikey.filter;

import com.aiinvoice.apikey.entity.ApiKey;
import com.aiinvoice.apikey.service.ApiKeyService;
import com.aiinvoice.auth.context.TenantContext;
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
@Order(10)
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("X-API-Key");
        if (header != null && !header.isBlank()) {
            Optional<ApiKey> key = apiKeyService.validateRawKey(header);
            if (key.isEmpty()) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write("{\"error\":\"Invalid or revoked API key\"}");
                return;
            }
            UUID orgId = key.get().getOrganizationId();
            req.setAttribute("apiKeyOrgId", orgId);
            TenantContext.set(orgId);
        }
        try {
            chain.doFilter(req, res);
        } finally {
            if (req.getHeader("X-API-Key") != null) TenantContext.clear();
        }
    }
}
