package com.ardom.automotive_event_api.storage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pre-signed S3 URL for downloading a file directly from the client")
public record PresignedDownloadResponse(

        @Schema(description = "Pre-signed S3 GET URL. Valid only until `expiresAt`.",
                example = "https://my-bucket.s3.eu-central-1.amazonaws.com/photos/42/a1b2c3.jpg?X-Amz-Signature=...")
        String downloadUrl,

        @Schema(description = "UTC timestamp when the pre-signed URL expires",
                example = "2026-06-01T12:30:00Z")
        Instant expiresAt

) {
}
