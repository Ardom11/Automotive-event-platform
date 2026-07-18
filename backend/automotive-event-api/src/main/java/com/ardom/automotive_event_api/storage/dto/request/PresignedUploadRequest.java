package com.ardom.automotive_event_api.storage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request body for generating a pre-signed S3 upload URL")
public record PresignedUploadRequest(

        @Schema(description = "Original filename including extension. Used to derive the file extension and validate it against the content type.",
                example = "porsche-front.jpg")
        @NotBlank
        String filename,

        @Schema(description = """
                MIME type of the file to be uploaded. Supported values:
                - `image/jpeg`
                - `image/png`
                - `image/webp`
                """,
                example = "image/jpeg")
        @NotBlank
        String contentType,

        @Schema(description = "File size in bytes. Must not exceed the configured maximum(5242880 bytes - 5 megabytes).",
                example = "2048576")
        @NotNull
        @Positive
        Integer filesize
) {
}
