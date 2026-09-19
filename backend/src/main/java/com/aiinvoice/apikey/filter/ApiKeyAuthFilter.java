package com.aiinvoice.apikey.filter;

import com.aiinvoice.apikey.entity.ApiKey;
import com.aiinvoice.apikey.service.ApiKeyService;
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
            req.setAttribute("apiKeyOrgId", key.get().getOrganizationId());
        }
        chain.doFilter(req, res);
    }
}
