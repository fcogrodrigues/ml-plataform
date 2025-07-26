package com.ifood.mlplatform.service;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class StorageService {

    @Value("${BUCKET_NAME}")
    private String bucket;

    @Value("${MINIO_ENDPOINT}")
    private String endpoint;

    @Value("${MINIO_ACCESS_KEY}")
    private String accessKey;

    @Value("${MINIO_SECRET_KEY}")
    private String secretKey;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        log.info("🔧 MinIO Config -> endpoint={}, bucket={}, accessKey={}", endpoint, bucket, accessKey);
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            log.info("✅ MinIO client initialized.");
        } catch (Exception e) {
            log.error("❌ Failed to initialize MinIO client: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO initialization error", e);
        }
    }

    public void upload(String key, Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(is, Files.size(filePath), -1)
                    .contentType("application/octet-stream")
                    .build()
            );
            log.info("✅ Uploaded {} to s3://{}/{}", filePath, bucket, key);
        } catch (Exception e) {
            log.error("❌ Upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Upload error", e);
        }
    }

    public InputStream download(String key) {
        try {
            log.info("⬇️ Downloading s3://{}/{}", bucket, key);
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Download failed: {}", e.getMessage(), e);
            throw new RuntimeException("Download error", e);
        }
    }
}
