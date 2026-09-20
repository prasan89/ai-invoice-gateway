package com.aiinvoice.copilot.service;

import com.aiinvoice.anomaly.repository.InvoiceAnomalyRepository;
import com.aiinvoice.copilot.dto.*;
import com.aiinvoice.copilot.entity.CopilotMessage;
import com.aiinvoice.copilot.entity.CopilotSession;
import com.aiinvoice.copilot.repository.CopilotMessageRepository;
import com.aiinvoice.copilot.repository.CopilotSessionRepository;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.Vendor;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import com.aiinvoice.invoice.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CopilotService {

    private final CopilotSessionRepository sessionRepo;
    private final CopilotMessageRepository messageRepo;
    private final InvoiceRepository invoiceRepo;
    private final VendorRepository vendorRepo;
    private final InvoiceAnomalyRepository anomalyRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${claude.api-key:}") private String claudeApiKey;
    @Value("${claude.model:claude-sonnet-4-6}") private String claudeModel;
    @Value("${hyperspace.api-key:}") private String hyperspaceApiKey;
    @Value("${hyperspace.base-url:}") private String hyperspaceBaseUrl;
    @Value("${hyperspace.model:claude-sonnet-4-6}") private String hyperspaceModel;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are an AI Finance Copilot embedded inside an invoice management platform.
        You help finance teams analyze their invoice data, detect anomalies, understand spending, and answer questions about vendors, GST, and cash flow.

        LIVE DATA SNAPSHOT (as of now):
        %s

        Rules:
        - Always answer using the live data above. Quote exact numbers, vendor names, and amounts from the data.
        - Use INR formatting: ₹1,23,456.78 (Indian numbering system).
        - Be concise and data-driven. If the data snapshot covers the question, answer directly. Do not say "check the dashboard" — you ARE the data.
        - If asked about something not in the snapshot, say you don't have that detail available.
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

        CopilotMessage userMsg = newMessage(sessionId, "USER", request.question());
        messageRepo.save(userMsg);

        List<Map<String, String>> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream()
            .map(m -> Map.of("role", m.getRole().toLowerCase(), "content", m.getContent()))
            .toList();

        // Build live data snapshot for this org
        InvoiceSnapshot snap = buildSnapshot(orgId);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, snap.toText());

        String answer = callClaude(systemPrompt, messages, snap, request.question());

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
        List<CopilotMessageDto> msgs = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream().map(this::toDto).toList();
        return new CopilotSessionDto(s.getId(), s.getTitle(), s.getUpdatedAt(), msgs);
    }

    // ── Live data snapshot ─────────────────────────────────────────────────────

    private InvoiceSnapshot buildSnapshot(UUID orgId) {
        List<Invoice> all = invoiceRepo.findAllWithLinesByOrgId(orgId);

        long total        = all.size();
        long approved     = all.stream().filter(i -> "APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name())).count();
        long review       = all.stream().filter(i -> "REVIEW_REQUIRED".equals(i.getStatus().name())).count();
        long rejected     = all.stream().filter(i -> "REJECTED".equals(i.getStatus().name())).count();
        long duplicates   = all.stream().filter(i -> i.getDuplicateScore() != null && i.getDuplicateScore() >= 80).count();

        BigDecimal totalSpend = all.stream()
            .filter(i -> "APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name()))
            .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGst = all.stream()
            .filter(i -> "APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name()))
            .map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCgst = all.stream()
            .filter(i -> "APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name()))
            .map(i -> i.getCgstAmount() != null ? i.getCgstAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSgst = all.stream()
            .filter(i -> "APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name()))
            .map(i -> i.getSgstAmount() != null ? i.getSgstAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIgst = all.stream()
            .filter(i -> "APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name()))
            .map(i -> i.getIgstAmount() != null ? i.getIgstAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Top 5 vendors by spend
        Map<String, BigDecimal> vendorSpend = all.stream()
            .filter(i -> ("APPROVED".equals(i.getStatus().name()) || "AUTO_APPROVED".equals(i.getStatus().name()))
                         && i.getSupplierName() != null)
            .collect(Collectors.groupingBy(
                Invoice::getSupplierName,
                Collectors.reducing(BigDecimal.ZERO,
                    i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO,
                    BigDecimal::add)));

        List<Map.Entry<String, BigDecimal>> topVendors = vendorSpend.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());

        // Recent 5 invoices (any status)
        List<Invoice> recent = all.stream().limit(5).collect(Collectors.toList());

        // Anomaly count
        long highRiskAnomalies = anomalyRepo.findHighRiskByOrgId(orgId).size();

        // Last rejected invoice reason
        Optional<Invoice> lastRejected = all.stream()
            .filter(i -> "REJECTED".equals(i.getStatus().name()))
            .findFirst();

        return new InvoiceSnapshot(total, approved, review, rejected, duplicates,
            totalSpend, totalGst, totalCgst, totalSgst, totalIgst,
            topVendors, recent, highRiskAnomalies, lastRejected.orElse(null));
    }

    private record InvoiceSnapshot(
        long total, long approved, long review, long rejected, long duplicates,
        BigDecimal totalSpend, BigDecimal totalGst, BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalIgst,
        List<Map.Entry<String, BigDecimal>> topVendors,
        List<Invoice> recentInvoices,
        long highRiskAnomalies,
        Invoice lastRejected
    ) {
        String toText() {
            StringBuilder sb = new StringBuilder();
            sb.append("INVOICE COUNTS:\n");
            sb.append("  Total: ").append(total).append("\n");
            sb.append("  Approved/Auto-approved: ").append(approved).append("\n");
            sb.append("  Pending review: ").append(review).append("\n");
            sb.append("  Rejected: ").append(rejected).append("\n");
            sb.append("  Flagged duplicates (score ≥80): ").append(duplicates).append("\n");
            sb.append("  High/critical anomalies: ").append(highRiskAnomalies).append("\n\n");

            sb.append("APPROVED SPEND:\n");
            sb.append("  Total spend (approved invoices): ₹").append(fmt(totalSpend)).append("\n");
            sb.append("  Total GST paid: ₹").append(fmt(totalGst)).append("\n");
            sb.append("  CGST: ₹").append(fmt(totalCgst))
              .append("  SGST: ₹").append(fmt(totalSgst))
              .append("  IGST: ₹").append(fmt(totalIgst)).append("\n\n");

            sb.append("TOP 5 VENDORS BY SPEND:\n");
            if (topVendors.isEmpty()) {
                sb.append("  No approved invoices yet.\n");
            } else {
                for (int i = 0; i < topVendors.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ")
                      .append(topVendors.get(i).getKey())
                      .append(" — ₹").append(fmt(topVendors.get(i).getValue())).append("\n");
                }
            }
            sb.append("\n");

            sb.append("RECENT INVOICES (last 5):\n");
            for (Invoice inv : recentInvoices) {
                sb.append("  • ").append(inv.getInvoiceNumber())
                  .append(" | ").append(inv.getSupplierName() != null ? inv.getSupplierName() : "Unknown supplier")
                  .append(" | ₹").append(inv.getTotalAmount() != null ? fmt(inv.getTotalAmount()) : "0")
                  .append(" | ").append(inv.getStatus())
                  .append(inv.getArithmeticStatus() != null ? " | Arithmetic: " + inv.getArithmeticStatus() : "")
                  .append("\n");
            }
            sb.append("\n");

            if (lastRejected != null) {
                sb.append("LAST REJECTED INVOICE:\n");
                sb.append("  ").append(lastRejected.getInvoiceNumber())
                  .append(" from ").append(lastRejected.getSupplierName() != null ? lastRejected.getSupplierName() : "Unknown")
                  .append(" — Reason: ").append(lastRejected.getValidationMessage() != null ? lastRejected.getValidationMessage() : "No reason recorded")
                  .append("\n");
            }

            return sb.toString();
        }

        private static String fmt(BigDecimal v) {
            if (v == null) return "0.00";
            return String.format("%,.2f", v.setScale(2, RoundingMode.HALF_UP));
        }
    }

    // ── AI call ────────────────────────────────────────────────────────────────

    private String callClaude(String systemPrompt, List<Map<String, String>> messages, InvoiceSnapshot snap, String question) {
        if (!hyperspaceApiKey.isBlank() && !hyperspaceBaseUrl.isBlank()) {
            return callApi(hyperspaceBaseUrl + "/v1/messages", hyperspaceApiKey, hyperspaceModel, systemPrompt, messages);
        }
        if (!claudeApiKey.isBlank()) {
            return callApi("https://api.anthropic.com/v1/messages", claudeApiKey, claudeModel, systemPrompt, messages);
        }
        return generateOfflineAnswer(snap, question);
    }

    private String callApi(String endpoint, String apiKey, String model, String systemPrompt, List<Map<String, String>> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", 1024);
            body.put("system", systemPrompt);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                List<?> content = (List<?>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) content.get(0);
                    return (String) first.get("text");
                }
            }
        } catch (Exception e) {
            log.warn("AI API call to {} failed, using offline mode: {}", endpoint, e.getMessage());
        }
        return generateOfflineAnswer(null, "");
    }

    // ── Smart offline answers using live data ──────────────────────────────────

    private String generateOfflineAnswer(InvoiceSnapshot snap, String question) {
        if (snap == null) return "I'm your AI Finance Copilot. How can I help you today?";

        String q = question.toLowerCase();

        if (q.contains("gst") || q.contains("tax")) {
            return String.format(
                "Based on your approved invoices, here is the GST breakdown:\n\n" +
                "• Total GST paid: ₹%s\n" +
                "• CGST: ₹%s\n" +
                "• SGST: ₹%s\n" +
                "• IGST: ₹%s\n\n" +
                "This is across %d approved invoices with a total spend of ₹%s.",
                InvoiceSnapshot.fmt(snap.totalGst()),
                InvoiceSnapshot.fmt(snap.totalCgst()),
                InvoiceSnapshot.fmt(snap.totalSgst()),
                InvoiceSnapshot.fmt(snap.totalIgst()),
                snap.approved(),
                InvoiceSnapshot.fmt(snap.totalSpend())
            );
        }

        if (q.contains("vendor") || q.contains("supplier") || q.contains("highest spend") || q.contains("top vendor")) {
            if (snap.topVendors().isEmpty()) {
                return "No approved invoices found yet, so vendor spend data is not available.";
            }
            StringBuilder sb = new StringBuilder("Here are your top vendors by spend:\n\n");
            for (int i = 0; i < snap.topVendors().size(); i++) {
                sb.append(String.format("%d. %s — ₹%s\n",
                    i + 1,
                    snap.topVendors().get(i).getKey(),
                    InvoiceSnapshot.fmt(snap.topVendors().get(i).getValue())));
            }
            sb.append(String.format("\nTotal approved spend across all vendors: ₹%s", InvoiceSnapshot.fmt(snap.totalSpend())));
            return sb.toString();
        }

        if (q.contains("duplicate")) {
            return String.format(
                "Your duplicate detection engine has flagged %d invoice(s) with a similarity score ≥ 80%%.\n\n" +
                "Out of %d total invoices, %d are pending review — some may be potential duplicates waiting for action.",
                snap.duplicates(), snap.total(), snap.review()
            );
        }

        if (q.contains("reject") || q.contains("failed")) {
            String base = String.format("%d invoice(s) have been rejected out of %d total.", snap.rejected(), snap.total());
            if (snap.lastRejected() != null) {
                base += String.format("\n\nLast rejected: %s from %s\nReason: %s",
                    snap.lastRejected().getInvoiceNumber(),
                    snap.lastRejected().getSupplierName() != null ? snap.lastRejected().getSupplierName() : "Unknown supplier",
                    snap.lastRejected().getValidationMessage() != null ? snap.lastRejected().getValidationMessage() : "No reason recorded");
            }
            return base;
        }

        if (q.contains("anomal") || q.contains("risk") || q.contains("unusual") || q.contains("suspicious")) {
            return String.format(
                "There are currently %d high or critical risk anomalies detected across your invoices.\n\n" +
                "Out of %d total invoices, %d are pending review. Visit the Anomalies tab for full details.",
                snap.highRiskAnomalies(), snap.total(), snap.review()
            );
        }

        if (q.contains("spend") || q.contains("total") || q.contains("how much") || q.contains("dashboard") || q.contains("summary") || q.contains("overview")) {
            return String.format(
                "Here is your invoice dashboard summary:\n\n" +
                "• Total invoices: %d\n" +
                "• Approved: %d\n" +
                "• Pending review: %d\n" +
                "• Rejected: %d\n" +
                "• Flagged duplicates: %d\n" +
                "• High-risk anomalies: %d\n\n" +
                "• Total approved spend: ₹%s\n" +
                "• Total GST paid: ₹%s",
                snap.total(), snap.approved(), snap.review(), snap.rejected(),
                snap.duplicates(), snap.highRiskAnomalies(),
                InvoiceSnapshot.fmt(snap.totalSpend()),
                InvoiceSnapshot.fmt(snap.totalGst())
            );
        }

        if (q.contains("recent") || q.contains("latest") || q.contains("last invoice")) {
            if (snap.recentInvoices().isEmpty()) return "No invoices found yet.";
            StringBuilder sb = new StringBuilder("Here are your 5 most recent invoices:\n\n");
            for (Invoice inv : snap.recentInvoices()) {
                sb.append(String.format("• %s | %s | ₹%s | %s\n",
                    inv.getInvoiceNumber(),
                    inv.getSupplierName() != null ? inv.getSupplierName() : "Unknown",
                    inv.getTotalAmount() != null ? InvoiceSnapshot.fmt(inv.getTotalAmount()) : "0",
                    inv.getStatus()));
            }
            return sb.toString();
        }

        // General greeting / fallback with real counts
        return String.format(
            "I'm your AI Finance Copilot. Here's a quick snapshot of your account:\n\n" +
            "• %d invoices total — %d approved, %d pending review, %d rejected\n" +
            "• Total approved spend: ₹%s\n" +
            "• Total GST paid: ₹%s\n\n" +
            "Ask me about vendors, GST, duplicates, anomalies, spend analysis, or any specific invoice.",
            snap.total(), snap.approved(), snap.review(), snap.rejected(),
            InvoiceSnapshot.fmt(snap.totalSpend()),
            InvoiceSnapshot.fmt(snap.totalGst())
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

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
