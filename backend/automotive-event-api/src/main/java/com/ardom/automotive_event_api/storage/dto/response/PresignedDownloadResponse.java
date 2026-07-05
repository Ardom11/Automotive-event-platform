package com.ardom.automotive_event_api.storage.dto.response;

import java.time.Instant;

public record PresignedDownloadResponse(
        String downloadUrl,
        Instant expiresAt
) {
}
