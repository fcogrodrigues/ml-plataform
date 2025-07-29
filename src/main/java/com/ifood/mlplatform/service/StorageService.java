package com.ifood.mlplatform.service;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ifood.mlplatform.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    @Value("${BUCKET_NAME}")
    private String bucket;

    public void upload(String objectName, Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            long size = Files.size(filePath);
            upload(objectName, is, size, "application/octet-stream");
            log.info("✅ Uploaded file `{}` as `{}`", filePath, objectName);
        } catch (IOException e) {
            log.error("❌ Failed to read file `{}`: {}", filePath, e.getMessage(), e);
            throw new StorageException("Could not read file for upload: " + filePath, e);
        }
    }

    public void upload(String objectName, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("✅ Uploaded stream as `{}` ({} bytes)", objectName, size);
        } catch (Exception e) {
            log.error("❌ Upload of `{}` failed: {}", objectName, e.getMessage(), e);
            throw new StorageException("Failed to upload object: " + objectName, e);
        }
    }

    public InputStream download(String objectName) {
        try {
            // optional: verify existence
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            log.info("⬇️ Downloading `{}`", objectName);
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Download of `{}` failed: {}", objectName, e.getMessage(), e);
            throw new StorageException("Failed to download object: " + objectName, e);
        }
    }
}
