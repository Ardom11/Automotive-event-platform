package com.ardom.automotive_event_api.storage.dto.response;

import java.time.Instant;

public record PresignedUploadResponse(
        String uploadUrl,
        String key,
        Instant expiresAt
) {
}
