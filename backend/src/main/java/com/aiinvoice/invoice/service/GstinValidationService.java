package com.aiinvoice.invoice.service;

import com.aiinvoice.invoice.domain.GstinValidationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates GSTINs using format check + Luhn-style checksum.
 *
 * TEST mode: set gstin.validation.mode=test and list synthetic GSTINs in
 * gstin.test-allowlist (comma-separated). Those GSTINs bypass the checksum
 * and return VALID, letting the auto-approval flow be tested end-to-end
 * without weakening production validation for all other GSTINs.
 */
@Service
public class GstinValidationService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile(
        "^(0[1-9]|[1-2][0-9]|3[0-7]|97|98|99)[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private static final String CHECKSUM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final boolean testMode;
    private final Set<String> testAllowlist;

    public GstinValidationService(
            @Value("${gstin.validation.mode:production}") String mode,
            @Value("${gstin.test-allowlist:}") String allowlist) {
        this.testMode = "test".equalsIgnoreCase(mode);
        this.testAllowlist = Arrays.stream(allowlist.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }

    public GstinValidationStatus validate(String gstin) {
        if (gstin == null || gstin.isBlank()) return GstinValidationStatus.NOT_PROVIDED;
        String g = gstin.trim().toUpperCase();

        if (testMode && testAllowlist.contains(g)) return GstinValidationStatus.VALID;

        if (!GSTIN_PATTERN.matcher(g).matches()) return GstinValidationStatus.INVALID;
        return validateChecksum(g) ? GstinValidationStatus.VALID : GstinValidationStatus.INVALID;
    }

    private boolean validateChecksum(String gstin) {
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int code = CHECKSUM_CHARS.indexOf(gstin.charAt(i));
            if (code < 0) return false;
            int product = code * (i % 2 == 0 ? 1 : 2);
            sum += (product / 36) + (product % 36);
        }
        int check = (36 - (sum % 36)) % 36;
        return gstin.charAt(14) == CHECKSUM_CHARS.charAt(check);
    }
}
