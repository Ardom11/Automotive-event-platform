package com.ardom.automotive_event_api.storage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignedUploadRequest(
        @NotBlank String filename,
        @NotBlank String contentType
) {
}
