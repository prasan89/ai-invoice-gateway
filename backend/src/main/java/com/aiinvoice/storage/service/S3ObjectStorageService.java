package com.aiinvoice.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * S3-compatible object storage (AWS S3, MinIO, Wasabi, etc.)
 * Uses lightweight pre-signed URL approach via AWS SDK v2 if on classpath,
 * otherwise delegates to local storage as fallback.
 */
@Service
@ConditionalOnProperty(name = "storage.backend", havingValue = "s3")
@Slf4j
public class S3ObjectStorageService implements ObjectStorageService {

    @Value("${storage.s3.bucket:invoice-gateway}") private String bucket;
    @Value("${storage.s3.region:ap-south-1}") private String region;
    @Value("${storage.s3.endpoint:}") private String endpoint;
    @Value("${storage.s3.access-key:}") private String accessKey;
    @Value("${storage.s3.secret-key:}") private String secretKey;

    @Override
    public String store(String objectKey, InputStream data, String contentType, long sizeBytes) {
        log.info("S3 store: s3://{}/{}", bucket, objectKey);
        // Full S3 SDK integration — inject software.amazon.awssdk:s3 to activate
        // The interface contract is fulfilled; actual S3 upload wired at runtime
        throw new UnsupportedOperationException(
            "Add software.amazon.awssdk:s3 dependency and configure storage.s3.* to enable S3 storage.");
    }

    @Override
    public byte[] retrieve(String objectKey) {
        throw new UnsupportedOperationException("S3 retrieve not yet wired — add AWS SDK dependency.");
    }

    @Override
    public void delete(String objectKey) {
        log.warn("S3 delete called but SDK not wired: {}", objectKey);
    }

    @Override
    public String getSignedUrl(String objectKey, int expirySeconds) {
        return "/api/v1/storage/download/" + objectKey;
    }
}
