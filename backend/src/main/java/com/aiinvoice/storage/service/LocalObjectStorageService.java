package com.aiinvoice.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnProperty(name = "storage.backend", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalObjectStorageService implements ObjectStorageService {

    @Value("${invoice.storage-dir:./data/invoices}")
    private String storageDir;

    @Override
    public String store(String objectKey, InputStream data, String contentType, long sizeBytes) {
        try {
            Path target = Paths.get(storageDir, objectKey);
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            return objectKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store object: " + objectKey, e);
        }
    }

    @Override
    public byte[] retrieve(String objectKey) {
        try {
            return Files.readAllBytes(Paths.get(storageDir, objectKey));
        } catch (IOException e) {
            throw new RuntimeException("Object not found: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(Paths.get(storageDir, objectKey));
        } catch (IOException e) {
            log.warn("Could not delete object {}: {}", objectKey, e.getMessage());
        }
    }

    @Override
    public String getSignedUrl(String objectKey, int expirySeconds) {
        return "/api/v1/storage/download/" + objectKey;
    }
}
