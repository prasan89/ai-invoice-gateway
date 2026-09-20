package com.aiinvoice.anomaly.service;

import com.aiinvoice.anomaly.dto.AnomalyDto;
import com.aiinvoice.anomaly.entity.InvoiceAnomaly;
import com.aiinvoice.anomaly.entity.VendorBaseline;
import com.aiinvoice.anomaly.repository.InvoiceAnomalyRepository;
import com.aiinvoice.anomaly.repository.VendorBaselineRepository;
import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.entity.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnomalyDetectionServiceTest {

    @Mock private InvoiceAnomalyRepository anomalyRepo;
    @Mock private VendorBaselineRepository baselineRepo;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyDetectionService(anomalyRepo, baselineRepo);
        when(anomalyRepo.findByInvoiceId(any())).thenReturn(Optional.empty());
        when(anomalyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(baselineRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Default: no baseline
        when(baselineRepo.findByOrganizationIdAndSupplierGstin(any(), any()))
            .thenReturn(Optional.empty());
    }

    private Invoice baseInvoice() {
        Invoice inv = new Invoice();
        inv.setId(UUID.randomUUID());
        inv.setOrganizationId(UUID.randomUUID());
        inv.setSupplierGstin("29AABCT1332L1ZA");
        inv.setSupplierName("Test Supplier");
        inv.setTotalAmount(new BigDecimal("1000.00"));
        inv.setTaxAmount(new BigDecimal("150.00"));
        return inv;
    }

    private VendorBaseline baseline(int count, BigDecimal avg) {
        VendorBaseline b = new VendorBaseline();
        b.setInvoiceCount(count);
        b.setAvgAmount(avg);
        return b;
    }

    @Test
    void newVendorNoBaseline_addsNewVendorType() {
        AnomalyDto result = service.detectAndSave(baseInvoice());
        assertTrue(result.anomalyTypes().contains("NEW_VENDOR"));
        assertTrue(result.riskScore() >= 20);
    }

    @Test
    void vendorWithFewInvoices_addsNewVendorType() {
        VendorBaseline b = baseline(2, new BigDecimal("800.00")); // count < 3
        when(baselineRepo.findByOrganizationIdAndSupplierGstin(any(), any()))
            .thenReturn(Optional.of(b));

        AnomalyDto result = service.detectAndSave(baseInvoice());
        assertTrue(result.anomalyTypes().contains("NEW_VENDOR"));
    }

    @Test
    void amountSpike_3sigmaAboveAvg_addsAmountSpikeType() {
        VendorBaseline b = baseline(10, new BigDecimal("500.00"));
        b.setStddevAmount(new BigDecimal("50.00")); // threshold = 500 + 3*50 = 650
        when(baselineRepo.findByOrganizationIdAndSupplierGstin(any(), any()))
            .thenReturn(Optional.of(b));

        Invoice inv = baseInvoice();
        inv.setTotalAmount(new BigDecimal("1000.00")); // > 650 → spike

        AnomalyDto result = service.detectAndSave(inv);
        assertTrue(result.anomalyTypes().contains("AMOUNT_SPIKE"));
        assertTrue(result.riskScore() >= 30);
    }

    @Test
    void normalAmount_noAmountSpike() {
        VendorBaseline b = baseline(10, new BigDecimal("1000.00"));
        b.setStddevAmount(new BigDecimal("200.00")); // threshold = 1000 + 600 = 1600
        when(baselineRepo.findByOrganizationIdAndSupplierGstin(any(), any()))
            .thenReturn(Optional.of(b));

        Invoice inv = baseInvoice();
        inv.setTotalAmount(new BigDecimal("1100.00")); // < 1600 → no spike

        assertFalse(service.detectAndSave(inv).anomalyTypes().contains("AMOUNT_SPIKE"));
    }

    @Test
    void gstRateDeviation_addsGstMismatchType() {
        VendorBaseline b = baseline(10, new BigDecimal("500.00"));
        b.setTypicalGstRate(new BigDecimal("18.00"));
        when(baselineRepo.findByOrganizationIdAndSupplierGstin(any(), any()))
            .thenReturn(Optional.of(b));

        Invoice inv = baseInvoice();
        // total=1000, tax=400 → effective rate = 400/600 * 100 ≈ 66.7% (>> 18+5)
        inv.setTaxAmount(new BigDecimal("400.00"));
        inv.setTotalAmount(new BigDecimal("1000.00"));

        assertTrue(service.detectAndSave(inv).anomalyTypes().contains("GST_MISMATCH"));
    }

    @Test
    void duplicateScore90_addsDuplicateRiskWithHigherScore() {
        Invoice inv = baseInvoice();
        inv.setDuplicateScore(90);

        AnomalyDto result = service.detectAndSave(inv);
        assertTrue(result.anomalyTypes().contains("DUPLICATE_RISK"));
        assertTrue(result.riskScore() >= 40); // NEW_VENDOR(20) + DUPLICATE>=90(40) = 60
    }

    @Test
    void duplicateScore70_addsDuplicateRiskWithLowerScore() {
        Invoice inv = baseInvoice();
        inv.setDuplicateScore(70);

        assertTrue(service.detectAndSave(inv).anomalyTypes().contains("DUPLICATE_RISK"));
    }

    @Test
    void duplicateScoreBelow70_noDuplicateType() {
        Invoice inv = baseInvoice();
        inv.setDuplicateScore(69);

        assertFalse(service.detectAndSave(inv).anomalyTypes().contains("DUPLICATE_RISK"));
    }

    @Test
    void arithmeticFail_addsArithmeticFailType() {
        Invoice inv = baseInvoice();
        inv.setArithmeticStatus(ArithmeticStatus.FAIL);

        assertTrue(service.detectAndSave(inv).anomalyTypes().contains("ARITHMETIC_FAIL"));
    }

    @Test
    void lowConfidence_addsLowConfidenceType() {
        Invoice inv = baseInvoice();
        inv.setExtractionConfidence(new BigDecimal("0.30")); // < 0.5

        AnomalyDto result = service.detectAndSave(inv);
        assertTrue(result.anomalyTypes().contains("LOW_CONFIDENCE"));
        assertTrue(result.riskScore() >= 15);
    }

    @Test
    void highConfidence_noLowConfidenceType() {
        Invoice inv = baseInvoice();
        inv.setExtractionConfidence(new BigDecimal("0.95"));

        assertFalse(service.detectAndSave(inv).anomalyTypes().contains("LOW_CONFIDENCE"));
    }

    @Test
    void riskScore_cappedAt100() {
        Invoice inv = baseInvoice();
        inv.setDuplicateScore(95);
        inv.setArithmeticStatus(ArithmeticStatus.FAIL);
        inv.setExtractionConfidence(new BigDecimal("0.20"));

        assertTrue(service.detectAndSave(inv).riskScore() <= 100);
    }

    @Test
    void riskLevel_critical_when75orMore() {
        Invoice inv = baseInvoice();
        inv.setDuplicateScore(95);           // +40 (>=90)
        inv.setArithmeticStatus(ArithmeticStatus.FAIL); // +20
        inv.setExtractionConfidence(new BigDecimal("0.20")); // +15
        // NEW_VENDOR(20) + DUPLICATE(40) + ARITHMETIC(20) + CONFIDENCE(15) = 95 → CRITICAL

        assertEquals("CRITICAL", service.detectAndSave(inv).riskLevel());
    }

    @Test
    void riskLevel_low_whenNoSignals() {
        VendorBaseline b = baseline(10, new BigDecimal("1000.00"));
        when(baselineRepo.findByOrganizationIdAndSupplierGstin(any(), any()))
            .thenReturn(Optional.of(b));

        Invoice inv = baseInvoice();
        inv.setExtractionConfidence(new BigDecimal("0.99"));
        inv.setDuplicateScore(0);

        assertEquals("LOW", service.detectAndSave(inv).riskLevel());
    }

    @Test
    void existingAnomaly_isUpdated() {
        InvoiceAnomaly existing = new InvoiceAnomaly();
        existing.setId(UUID.randomUUID());
        UUID invoiceId = UUID.randomUUID();
        when(anomalyRepo.findByInvoiceId(invoiceId)).thenReturn(Optional.of(existing));

        Invoice inv = baseInvoice();
        inv.setId(invoiceId);

        service.detectAndSave(inv);
        verify(anomalyRepo, times(1)).save(existing);
    }

    @Test
    void nullSupplierGstin_doesNotThrow() {
        Invoice inv = baseInvoice();
        inv.setSupplierGstin(null);

        assertDoesNotThrow(() -> service.detectAndSave(inv));
    }
}
