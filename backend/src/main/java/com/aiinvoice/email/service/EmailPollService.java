package com.aiinvoice.email.service;

import com.aiinvoice.email.entity.EmailPollLog;
import com.aiinvoice.email.repository.EmailPollLogRepository;
import com.aiinvoice.invoice.service.InvoiceService;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
public class EmailPollService {

    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Value("${invoice.email.enabled:false}")
    private boolean enabled;

    @Value("${invoice.email.host:}")
    private String host;

    @Value("${invoice.email.port:993}")
    private int port;

    @Value("${invoice.email.user:}")
    private String user;

    @Value("${invoice.email.password:}")
    private String password;

    @Value("${invoice.email.folder:INBOX}")
    private String folder;

    @Value("${invoice.email.ssl:true}")
    private boolean ssl;

    private final InvoiceService invoiceService;
    private final EmailPollLogRepository logRepository;

    public EmailPollService(InvoiceService invoiceService, EmailPollLogRepository logRepository) {
        this.invoiceService = invoiceService;
        this.logRepository = logRepository;
    }

    /**
     * Poll every 5 minutes when enabled.
     */
    @Scheduled(fixedDelayString = "${invoice.email.poll-interval-ms:300000}")
    public void poll() {
        if (!enabled || host.isBlank() || user.isBlank()) {
            return;
        }

        EmailPollLog logEntry = new EmailPollLog();
        logEntry.setOrganizationId(DEMO_ORG);
        logEntry.setPolledAt(Instant.now());
        logEntry.setMailboxUser(user);

        int found = 0, created = 0, errors = 0;
        StringBuilder errDetail = new StringBuilder();

        Store store = null;
        Folder inbox = null;
        try {
            store = connectStore();
            inbox = store.getFolder(folder);
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new jakarta.mail.search.FlagTerm(
                new Flags(Flags.Flag.SEEN), false));
            found = messages.length;

            for (Message msg : messages) {
                try {
                    int processed = processMessage(msg);
                    created += processed;
                    msg.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception e) {
                    errors++;
                    errDetail.append(e.getMessage()).append("; ");
                    log.warn("Failed to process email message: {}", e.getMessage());
                }
            }

            logEntry.setStatus(errors == 0 ? "SUCCESS" : (created > 0 ? "PARTIAL" : "FAILED"));

        } catch (Exception e) {
            log.error("Email poll failed: {}", e.getMessage(), e);
            logEntry.setStatus("FAILED");
            errDetail.append(e.getMessage());
            errors++;
        } finally {
            closeQuietly(inbox, store);
        }

        logEntry.setMessagesFound(found);
        logEntry.setInvoicesCreated(created);
        logEntry.setErrors(errors);
        if (!errDetail.isEmpty()) {
            logEntry.setErrorDetail(errDetail.toString());
        }
        logRepository.save(logEntry);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Store connectStore() throws MessagingException {
        Properties props = new Properties();
        String protocol = ssl ? "imaps" : "imap";
        props.put("mail." + protocol + ".host", host);
        props.put("mail." + protocol + ".port", String.valueOf(port));
        if (ssl) {
            props.put("mail.imaps.ssl.enable", "true");
        }

        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        store.connect(host, user, password);
        return store;
    }

    /**
     * Extracts PDF/image attachments from an email and submits each as an invoice upload.
     * Returns the number of invoices created.
     */
    private int processMessage(Message msg) throws Exception {
        int count = 0;
        Object content = msg.getContent();

        if (content instanceof MimeMultipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart part = mp.getBodyPart(i);
                String disposition = part.getDisposition();
                String contentType = part.getContentType().toLowerCase();

                boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                    || Part.INLINE.equalsIgnoreCase(disposition);
                boolean isSupportedType = contentType.contains("pdf")
                    || contentType.contains("image/jpeg")
                    || contentType.contains("image/png");

                if (isAttachment && isSupportedType) {
                    byte[] bytes = readBytes(part.getInputStream());
                    String fileName = part.getFileName() != null ? part.getFileName() : "email-invoice.pdf";
                    String mimeType = contentType.split(";")[0].trim();

                    MultipartFile multipart = new ByteArrayMultipartFile(fileName, mimeType, bytes);
                    invoiceService.createFromUpload(multipart);
                    count++;
                }
            }
        }
        return count;
    }

    private byte[] readBytes(InputStream in) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private void closeQuietly(Folder f, Store s) {
        try { if (f != null && f.isOpen()) f.close(true); } catch (Exception ignored) {}
        try { if (s != null && s.isConnected()) s.close(); } catch (Exception ignored) {}
    }

    // ── simple MultipartFile adapter ─────────────────────────────────────────

    private record ByteArrayMultipartFile(String name, String contentType, byte[] data)
        implements MultipartFile {

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return data == null || data.length == 0; }
        @Override public long getSize() { return data == null ? 0 : data.length; }
        @Override public byte[] getBytes() { return data; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(data); }
        @Override public void transferTo(File dest) throws IOException {
            try (FileOutputStream out = new FileOutputStream(dest)) { out.write(data); }
        }
    }
}
