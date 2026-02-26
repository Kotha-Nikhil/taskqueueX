package com.taskqueuex.worker.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public StorageService(MinioClient minioClient, @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public String upload(String content, String fileName) {
        try {
            String objectName = UUID.randomUUID() + "/" + fileName;
            InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType("application/octet-stream")
                    .build()
            );

            logger.info("Uploaded file to MinIO: {}", objectName);
            return objectName;
        } catch (Exception e) {
            logger.error("Failed to upload to MinIO", e);
            throw new RuntimeException("Failed to upload to MinIO", e);
        }
    }

    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(7 * 24 * 60 * 60) // 7 days
                    .build()
            );
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for {}", objectName, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
