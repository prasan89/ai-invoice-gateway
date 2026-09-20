package com.aiinvoice.storage.service;

import java.io.InputStream;

public interface ObjectStorageService {
    String store(String objectKey, InputStream data, String contentType, long sizeBytes);
    byte[] retrieve(String objectKey);
    void delete(String objectKey);
    String getSignedUrl(String objectKey, int expirySeconds);
}
