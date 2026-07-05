package com.ardom.automotive_event_api.storage;

import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.storage.dto.response.PresignedUploadResponse;
import com.ardom.automotive_event_api.storage.exception.UnsupportedFiletypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration-minutes}")
    private int expirationMinutes;

    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            "image/jpeg", Set.of(".jpg", ".jpeg", ".jpe", ".jfif"),
            "image/png", Set.of(".png"),
            "image/webp", Set.of(".webp")
    );

    @Override
    public PresignedUploadResponse generateUploadUrl(Long userId, String originalFilename, String contentType) {
        if (!ALLOWED_EXTENSIONS.containsKey(contentType)) {
            throw new UnsupportedFiletypeException("Unsupported content type: " + contentType);
        }

        String key = buildKey(userId, originalFilename, contentType);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        Duration expiration = Duration.ofMinutes(expirationMinutes);

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUploadResponse(
                presignedRequest.url().toString(),
                key,
                Instant.now().plus(expiration)
        );
    }

    @Override
    public PresignedDownloadResponse generateDownloadUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        Duration expiry = Duration.ofMinutes(expirationMinutes);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        return new PresignedDownloadResponse(
                presignedRequest.url().toString(),
                Instant.now().plus(expiry)
        );
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete file: " + key, e);
        }
    }

    private String buildKey(Long userId, String originalFilename, String contentType) {
        String extension = getExtension(originalFilename);
        if (extension.isBlank()) {
            throw new IllegalArgumentException("File has no extension");
        }

        if (!ALLOWED_EXTENSIONS
                .getOrDefault(contentType, Set.of())
                .contains(extension)) {
            throw new IllegalArgumentException("Extension " + extension + " is not supported for content type " + contentType);
        }

        return "photos/%d/%s%s".formatted(userId, UUID.randomUUID(), extension);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "" : filename.substring(dotIndex);
    }
}
