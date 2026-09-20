package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.GstinValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GstinValidationServiceTest {

    private GstinValidationService service;

    @BeforeEach
    void setUp() {
        service = new GstinValidationService("production", "");
    }

    @Test
    void nullInput_returnsNotProvided() {
        assertEquals(GstinValidationStatus.NOT_PROVIDED, service.validate(null));
    }

    @Test
    void blankInput_returnsNotProvided() {
        assertEquals(GstinValidationStatus.NOT_PROVIDED, service.validate("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "29AABCT1332L1ZA",  // Karnataka — checksum verified
        "27AAACR5055K1Z7",  // Maharashtra — checksum verified
        "07AAGCR0376L1Z3",  // Delhi — checksum verified
    })
    void validGstins_returnValid(String gstin) {
        assertEquals(GstinValidationStatus.VALID, service.validate(gstin));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "INVALID",
        "00AABCT1332L1ZA",  // State code 00 not valid
        "29AABCT1332L1ZX",  // Wrong checksum digit
        "29AABCT1332L1",    // Too short (13 chars)
    })
    void invalidGstins_returnInvalid(String gstin) {
        assertEquals(GstinValidationStatus.INVALID, service.validate(gstin));
    }

    @Test
    void testMode_allowlistedGstin_returnsValid() {
        // "29TTEST0000T1ZA" is exactly 15 chars, in allowlist → bypasses pattern+checksum
        GstinValidationService testService = new GstinValidationService("test", "29TTEST0000T1ZA");
        assertEquals(GstinValidationStatus.VALID, testService.validate("29TTEST0000T1ZA"));
    }

    @Test
    void testMode_nonAllowlisted_stillValidatesNormally() {
        GstinValidationService testService = new GstinValidationService("test", "29TTEST0000T1ZA");
        assertEquals(GstinValidationStatus.INVALID, testService.validate("INVALID"));
    }

    @Test
    void ocr_wrongChecksum_returnsInvalid() {
        // Valid format but wrong checksum character → INVALID
        assertEquals(GstinValidationStatus.INVALID, service.validate("29AABCT1332L1ZX"));
    }

    @Test
    void wrongLength_tooLong_returnsInvalid() {
        assertEquals(GstinValidationStatus.INVALID, service.validate("29AABCT1332L1ZAXX"));
    }

    @Test
    void whitespaceAroundValidGstin_normalizedAndValidated() {
        // Leading/trailing whitespace stripped during normalize
        assertEquals(GstinValidationStatus.VALID, service.validate("  29AABCT1332L1ZA  "));
    }
}
