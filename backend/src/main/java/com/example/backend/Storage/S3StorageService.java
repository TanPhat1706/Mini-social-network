package com.example.backend.Storage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// Không dùng @Service ở đây — Bean được tạo bởi StorageConfig (theo điều kiện STORAGE_TYPE=s3)
public class S3StorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final String endpoint;
    private final String publicBaseUrl;

    public S3StorageService(
            String accessKey,
            String secretKey,
            String region,
            String bucketName) {
        this(accessKey, secretKey, region, bucketName, null, null);
    }

    public S3StorageService(
            String accessKey,
            String secretKey,
            String region,
            String bucketName,
            String endpoint,
            String publicBaseUrl) {
        this.bucketName = bucketName;
        this.region = region;
        this.endpoint = normalizeBaseUrl(endpoint);
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);

        var builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region));

        if (this.endpoint != null) {
            builder.endpointOverride(URI.create(this.endpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        this.s3Client = builder.build();
    }

    @Override
    public String storeFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }

        String originalFileName = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElse("file");
        String safeFileName = Paths.get(originalFileName).getFileName().toString();
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = buildObjectKey(normalizedDirectory, safeFileName);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))
                    .contentLength(file.getSize())
                    .cacheControl("public, max-age=31536000")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return buildFileUrl(objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public boolean isValidFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    public String buildFileUrl(String objectKey) {
        if (publicBaseUrl != null) {
            return publicBaseUrl + "/" + objectKey;
        }
        if (endpoint != null) {
            return endpoint + "/" + bucketName + "/" + objectKey;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
    }

    private String buildObjectKey(String directory, String safeFileName) {
        String fileName = UUID.randomUUID().toString() + "_" + safeFileName;
        if (directory == null || directory.isBlank()) {
            return fileName;
        }
        return directory + "/" + fileName;
    }

    private String normalizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "";
        }

        String normalized = directory.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (isDisabledValue(normalized)) {
            return null;
        }

        if (!hasScheme(normalized)) {
            normalized = "https://" + normalized;
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        URI uri = URI.create(normalized);
        if (uri.getScheme() == null || uri.getHost() == null || uri.getHost().isBlank()) {
            return null;
        }
        return normalized;
    }

    private boolean hasScheme(String value) {
        int schemeSeparatorIndex = value.indexOf("://");
        return schemeSeparatorIndex > 0;
    }

    private boolean isDisabledValue(String value) {
        String normalized = value.trim();
        return normalized.isEmpty()
                || "null".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)
                || normalized.startsWith("${");
    }
}
