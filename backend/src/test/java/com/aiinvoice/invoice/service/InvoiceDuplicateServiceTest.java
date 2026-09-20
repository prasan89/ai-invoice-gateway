package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceDuplicateServiceTest {

    @Mock
    private InvoiceRepository repository;

    private InvoiceDuplicateService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceDuplicateService(repository);
        // Default: no hash match, no candidates
        when(repository.findFirstByDocumentHashAndIdNot(any(), any())).thenReturn(Optional.empty());
        when(repository.findDuplicateCandidates(any(), any(), any(), any())).thenReturn(List.of());
    }

    private Invoice invoice(String invNum, String gstin, BigDecimal amount, LocalDate date) {
        Invoice inv = new Invoice();
        inv.setId(UUID.randomUUID());
        inv.setOrganizationId(UUID.randomUUID());
        inv.setInvoiceNumber(invNum);
        inv.setSupplierGstin(gstin);
        inv.setTotalAmount(amount);
        inv.setInvoiceDate(date);
        return inv;
    }

    @Test
    void exactHashMatch_returns100() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.now());
        incoming.setDocumentHash("abc123");

        Invoice existing = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.now());
        when(repository.findFirstByDocumentHashAndIdNot("abc123", incoming.getId()))
            .thenReturn(Optional.of(existing));

        var result = service.check(incoming);
        assertEquals(100, result.score());
        assertEquals(existing.getId(), result.duplicateInvoiceId());
    }

    @Test
    void noHashMatch_sameInvoiceNumberAndGstin_returns95() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        UUID orgId = incoming.getOrganizationId();

        Invoice candidate = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        candidate.setOrganizationId(orgId);
        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(candidate));

        assertEquals(95, service.check(incoming).score());
    }

    @Test
    void sameInvoiceNumberAndGstin_differentAmount_returns85() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        UUID orgId = incoming.getOrganizationId();

        Invoice candidate = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("2000"), LocalDate.of(2024, 1, 1));
        candidate.setOrganizationId(orgId);
        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(candidate));

        assertEquals(85, service.check(incoming).score());
    }

    @Test
    void sameInvoiceNumberAndGstin_differentDate_returns80() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        UUID orgId = incoming.getOrganizationId();

        Invoice candidate = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 2, 1));
        candidate.setOrganizationId(orgId);
        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(candidate));

        assertEquals(80, service.check(incoming).score());
    }

    @Test
    void sameInvoiceNumber_differentGstin_returns75() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        UUID orgId = incoming.getOrganizationId();

        Invoice candidate = invoice("INV-1", "27AAACR5055K1Z7", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        candidate.setOrganizationId(orgId);
        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(candidate));

        assertEquals(75, service.check(incoming).score());
    }

    @Test
    void sameSupplierSameDateAmountWithin1pct_returns70() {
        Invoice incoming = invoice(null, "29AABCT1332L1ZA", new BigDecimal("1000.00"), LocalDate.of(2024, 1, 1));
        UUID orgId = incoming.getOrganizationId();

        Invoice candidate = invoice("INV-X", "29AABCT1332L1ZA", new BigDecimal("1005.00"), LocalDate.of(2024, 1, 1));
        candidate.setOrganizationId(orgId);
        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(candidate));

        assertEquals(70, service.check(incoming).score());
    }

    @Test
    void sameSupplierSameExactAmount_returns65() {
        Invoice incoming = invoice(null, "29AABCT1332L1ZA", new BigDecimal("1000.00"), null);
        UUID orgId = incoming.getOrganizationId();

        Invoice candidate = invoice("INV-X", "29AABCT1332L1ZA", new BigDecimal("1000.00"), LocalDate.of(2024, 2, 5));
        candidate.setOrganizationId(orgId);
        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(candidate));

        assertEquals(65, service.check(incoming).score());
    }

    @Test
    void noCandidates_returnsScore0() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.now());
        var result = service.check(incoming);
        assertEquals(0, result.score());
        assertNull(result.duplicateInvoiceId());
    }

    @Test
    void computeHash_consistentForSameBytes() {
        byte[] data = "hello world".getBytes();
        String h1 = service.computeHash(data);
        String h2 = service.computeHash(data);
        assertEquals(h1, h2);
        assertEquals(64, h1.length()); // SHA-256 = 32 bytes = 64 hex chars
    }

    @Test
    void computeHash_differentForDifferentBytes() {
        assertNotEquals(service.computeHash("a".getBytes()), service.computeHash("b".getBytes()));
    }

    @Test
    void nullOrganizationId_returnsScore0WhenNoHash() {
        Invoice incoming = new Invoice();
        incoming.setId(UUID.randomUUID());
        incoming.setOrganizationId(null);

        var result = service.check(incoming);
        assertEquals(0, result.score());
    }

    @Test
    void multipleCandidates_bestScoreWins() {
        Invoice incoming = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 1, 1));
        UUID orgId = incoming.getOrganizationId();

        Invoice c1 = invoice("INV-1", "29AABCT1332L1ZA", new BigDecimal("1000"), LocalDate.of(2024, 3, 1)); // score 80
        c1.setOrganizationId(orgId);
        Invoice c2 = invoice("INV-1", "27AAACR5055K1Z7", new BigDecimal("1000"), LocalDate.of(2024, 1, 1)); // score 75
        c2.setOrganizationId(orgId);

        when(repository.findDuplicateCandidates(eq(orgId), eq(incoming.getId()), any(), any()))
            .thenReturn(List.of(c1, c2));

        var result = service.check(incoming);
        assertEquals(80, result.score());
        assertEquals(c1.getId(), result.duplicateInvoiceId());
    }
}
