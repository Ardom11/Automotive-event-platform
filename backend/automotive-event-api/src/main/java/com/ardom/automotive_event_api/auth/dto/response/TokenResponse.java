package com.ardom.automotive_event_api.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body for returning access token")
public record TokenResponse(
        String accessToken
) {
}
