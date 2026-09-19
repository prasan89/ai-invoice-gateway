package com.aiinvoice.apikey.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(11)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String key = resolveKey(req);
        if (key != null && isRateLimited(key)) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> hits = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < now - WINDOW_MS) hits.pollFirst();
            if (hits.size() >= MAX_REQUESTS) return true;
            hits.addLast(now);
            return false;
        }
    }

    private String resolveKey(HttpServletRequest req) {
        String apiKey = req.getHeader("X-API-Key");
        if (apiKey != null) return "key:" + apiKey;
        String path = req.getRequestURI();
        if (path.startsWith("/api/")) return "ip:" + req.getRemoteAddr();
        return null;
    }
}
