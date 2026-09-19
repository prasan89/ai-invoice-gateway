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
        String g = normalize(gstin);
        if (g == null) return GstinValidationStatus.INVALID;

        if (testMode && testAllowlist.contains(g)) return GstinValidationStatus.VALID;

        if (!GSTIN_PATTERN.matcher(g).matches()) return GstinValidationStatus.INVALID;
        return validateChecksum(g) ? GstinValidationStatus.VALID : GstinValidationStatus.INVALID;
    }

    /**
     * Normalizes an AI-extracted GSTIN string before validation:
     * - Strip whitespace, dots, dashes (OCR artifacts)
     * - Uppercase
     * - Fix common OCR confusions in the alphabetic positions:
     *     O→0 and I→1 in the numeric segments (positions 2-5, 10)
     *     0→O and 1→I in the alphabetic segments (positions 6-9, 11, 14)
     * Returns null if the result is not 15 characters (unrecoverable).
     */
    private String normalize(String raw) {
        String s = raw.trim().toUpperCase().replaceAll("[\\s.\\-]", "");
        if (s.length() != 15) return null;

        char[] c = s.toCharArray();
        // Positions 0-1: state code digits — O→0, I→1
        for (int i = 0; i <= 1; i++) { if (c[i] == 'O') c[i] = '0'; if (c[i] == 'I') c[i] = '1'; }
        // Positions 2-6: PAN letters (5 chars) — 0→O, 1→I
        for (int i = 2; i <= 6; i++) { if (c[i] == '0') c[i] = 'O'; if (c[i] == '1') c[i] = 'I'; }
        // Positions 7-10: PAN digits (4 chars) — O→0, I→1
        for (int i = 7; i <= 10; i++) { if (c[i] == 'O') c[i] = '0'; if (c[i] == 'I') c[i] = '1'; }
        // Position 11: PAN check letter — 0→O, 1→I
        if (c[11] == '0') c[11] = 'O'; if (c[11] == '1') c[11] = 'I';
        // Position 12: entity number (alphanumeric) — leave as-is
        // Position 13: always 'Z' — fix obvious OCR: 2→Z is too risky, just uppercase
        // Position 14: checksum (alphanumeric) — leave as-is
        return new String(c);
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
