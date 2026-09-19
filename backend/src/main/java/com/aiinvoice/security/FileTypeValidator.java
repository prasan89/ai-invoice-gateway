package com.aiinvoice.security;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Component
public class FileTypeValidator {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};         // %PDF
    private static final byte[] PNG_MAGIC = {(byte)0x89, 0x50, 0x4E, 0x47};  // .PNG
    private static final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF}; // JFIF

    public boolean isAllowed(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(8);
            return matchesMagic(header, PDF_MAGIC) ||
                    matchesMagic(header, PNG_MAGIC) ||
                    matchesMagic(header, JPEG_MAGIC);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean matchesMagic(byte[] header, byte[] magic) {
        if (header.length < magic.length) return false;
        return Arrays.equals(Arrays.copyOf(header, magic.length), magic);
    }
}
