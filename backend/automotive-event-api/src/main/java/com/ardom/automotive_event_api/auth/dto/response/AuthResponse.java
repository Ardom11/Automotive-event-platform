package com.ardom.automotive_event_api.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body for returning tokens")
public record AuthResponse(
        @Schema(description = "Access token to return to user")
        String accessToken,

        @Schema(description = "Refresh token to set in as cookie")
        String refreshToken
) {
}
