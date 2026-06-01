package com.ardom.automotive_event_api.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
