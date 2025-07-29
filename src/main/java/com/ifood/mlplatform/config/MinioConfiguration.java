package com.ifood.mlplatform.config;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ifood.mlplatform.exception.StorageException;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class MinioConfiguration {

    @Value("${MINIO_ENDPOINT}")
    private String endpoint;

    @Value("${MINIO_ACCESS_KEY}")
    private String accessKey;

    @Value("${MINIO_SECRET_KEY}")
    private String secretKey;

    @Value("${BUCKET_NAME}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("✅ Created bucket `{}`", bucketName);
            } else {
                log.info("✅ Bucket `{}` already exists", bucketName);
            }
        } catch (Exception e) {
            log.error("❌ Could not verify or create bucket `{}`: {}", bucketName, e.getMessage(), e);
            throw new StorageException("Failed to prepare bucket: " + bucketName, e);
        }
        return client;
    }
}
