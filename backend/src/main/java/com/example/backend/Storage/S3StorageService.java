package com.example.backend.Storage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public S3StorageService(
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey,
            @Value("${aws.region}") String region,
            @Value("${aws.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        this.region = region;
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .build();
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }

        String originalFileName = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElse("file");
        String safeFileName = Paths.get(originalFileName).getFileName().toString();
        String objectKey = UUID.randomUUID().toString() + "_" + safeFileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
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
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
    }
}
