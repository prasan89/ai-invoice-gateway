package com.aiinvoice.copilot.service;

import com.aiinvoice.copilot.dto.*;
import com.aiinvoice.copilot.entity.CopilotMessage;
import com.aiinvoice.copilot.entity.CopilotSession;
import com.aiinvoice.copilot.repository.CopilotMessageRepository;
import com.aiinvoice.copilot.repository.CopilotSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CopilotService {

    private final CopilotSessionRepository sessionRepo;
    private final CopilotMessageRepository messageRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${claude.api-key:}") private String claudeApiKey;
    @Value("${claude.model:claude-sonnet-4-6}") private String claudeModel;

    private static final String SYSTEM_PROMPT = """
        You are an AI Finance Copilot embedded inside an invoice management platform.
        You help finance teams analyze their invoice data, detect anomalies, understand spending, and answer questions about vendors, GST, and cash flow.

        You have access to aggregated invoice statistics. When asked analytical questions, provide clear, concise insights.
        For questions about specific invoices or amounts, be precise and use INR currency formatting (₹).

        Common tasks you can help with:
        - Spend analysis by vendor, category, or time period
        - GST paid/input credit analysis
        - Duplicate invoice detection summaries
        - Cash flow insights and payment due analysis
        - Vendor behavior patterns
        - "Why was this invoice rejected?" explanations

        Be conversational, helpful, and data-driven. If you cannot answer without more data, say so clearly.
        """;

    @Transactional
    public CopilotSessionDto ask(UUID orgId, UUID userId, CopilotAskRequest request) {
        UUID sessionId = resolveSessionId(request.sessionId());
        CopilotSession session = sessionRepo.findById(sessionId).orElseGet(() -> {
            CopilotSession s = new CopilotSession();
            s.setId(sessionId);
            s.setOrganizationId(orgId);
            s.setUserId(userId);
            s.setTitle(truncate(request.question(), 80));
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return sessionRepo.save(s);
        });

        // Save user message
        CopilotMessage userMsg = newMessage(sessionId, "USER", request.question());
        messageRepo.save(userMsg);

        // Build conversation history
        List<Map<String, String>> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream()
            .map(m -> Map.of("role", m.getRole().toLowerCase(), "content", m.getContent()))
            .toList();

        String answer = callClaude(messages);

        CopilotMessage assistantMsg = newMessage(sessionId, "ASSISTANT", answer);
        messageRepo.save(assistantMsg);

        session.setUpdatedAt(Instant.now());
        sessionRepo.save(session);

        List<CopilotMessageDto> allMessages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream().map(this::toDto).toList();
        return new CopilotSessionDto(session.getId(), session.getTitle(), session.getUpdatedAt(), allMessages);
    }

    public List<CopilotSessionDto> listSessions(UUID orgId) {
        return sessionRepo.findByOrganizationIdOrderByUpdatedAtDesc(orgId)
            .stream().map(s -> new CopilotSessionDto(s.getId(), s.getTitle(), s.getUpdatedAt(),
                messageRepo.findBySessionIdOrderByCreatedAtAsc(s.getId()).stream().map(this::toDto).toList()))
            .toList();
    }

    public CopilotSessionDto getSession(UUID sessionId) {
        CopilotSession s = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new NoSuchElementException("Session not found"));
        List<CopilotMessageDto> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream().map(this::toDto).toList();
        return new CopilotSessionDto(s.getId(), s.getTitle(), s.getUpdatedAt(), messages);
    }

    private String callClaude(List<Map<String, String>> messages) {
        if (claudeApiKey.isBlank()) return generateOfflineAnswer(messages);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", claudeApiKey);
            headers.set("anthropic-version", "2023-06-01");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", claudeModel);
            body.put("max_tokens", 1024);
            body.put("system", SYSTEM_PROMPT);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.anthropic.com/v1/messages", HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                List<?> content = (List<?>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) content.get(0);
                    return (String) first.get("text");
                }
            }
        } catch (Exception e) {
            log.warn("Claude API call failed, using offline mode: {}", e.getMessage());
        }
        return generateOfflineAnswer(messages);
    }

    private String generateOfflineAnswer(List<Map<String, String>> messages) {
        String lastQuestion = messages.isEmpty() ? "" :
            messages.get(messages.size() - 1).getOrDefault("content", "").toLowerCase();

        if (lastQuestion.contains("gst")) return "Based on your invoice data, I can help analyze GST payments. To give you precise figures, please ensure your invoices have been processed through the GST validation engine. Your dashboard shows a breakdown of CGST, SGST, and IGST across all approved invoices.";
        if (lastQuestion.contains("spend") || lastQuestion.contains("total")) return "Your total approved invoice spend is visible on the main dashboard. For a detailed vendor-wise breakdown, I can cross-reference your approved invoices. The top vendors by spend are typically shown in the dashboard stats.";
        if (lastQuestion.contains("duplicate")) return "Your duplicate detection engine flags invoices with a similarity score above 70%. High-risk duplicates (score ≥ 90%) are automatically flagged for review. You can see flagged duplicates in the review queue.";
        if (lastQuestion.contains("vendor")) return "Vendor analysis is available through the vendor intelligence module. Each vendor builds a baseline over time — tracking average invoice amounts, GST rates, and frequency. Unusual deviations are flagged as anomalies.";
        if (lastQuestion.contains("reject")) return "Invoices are rejected when they fail validation rules: arithmetic mismatch, invalid GSTIN, duplicate detection, or manual reviewer rejection. Check the invoice event timeline for the specific rejection reason.";
        return "I'm your AI Finance Copilot. I can help you analyze invoice data, track GST payments, identify anomalies, and answer questions about your vendors and spending. What would you like to know?";
    }

    private UUID resolveSessionId(String sessionIdStr) {
        if (sessionIdStr == null || sessionIdStr.isBlank()) return UUID.randomUUID();
        try { return UUID.fromString(sessionIdStr); } catch (Exception e) { return UUID.randomUUID(); }
    }

    private CopilotMessage newMessage(UUID sessionId, String role, String content) {
        CopilotMessage m = new CopilotMessage();
        m.setId(UUID.randomUUID());
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(Instant.now());
        return m;
    }

    private CopilotMessageDto toDto(CopilotMessage m) {
        return new CopilotMessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt());
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max) + "…" : s);
    }
}
