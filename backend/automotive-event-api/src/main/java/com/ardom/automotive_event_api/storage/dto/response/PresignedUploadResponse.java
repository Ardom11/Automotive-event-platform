package com.ardom.automotive_event_api.storage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pre-signed S3 URL for uploading a file directly from the client")
public record PresignedUploadResponse(

        @Schema(description = """
                Pre-signed S3 PUT URL. The client must upload the file directly to this URL
                using an HTTP PUT request with the matching `Content-Type` header.
                The URL is valid until `expiresAt`.
                """,
                example = "https://my-bucket.s3.eu-central-1.amazonaws.com/photos/42/a1b2c3.jpg?X-Amz-Signature=...")
        String uploadUrl,

        @Schema(description = """
                S3 object key assigned to this file. Format: `photos/<userId>/<uuid>.<ext>`.
                Store this key and pass it in `CarDto.photoKeys` when submitting car photos.
                """,
                example = "photos/42/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg")
        String key,

        @Schema(description = "UTC timestamp when the pre-signed URL expires",
                example = "2026-06-01T12:30:00Z")
        Instant expiresAt

) {
}
