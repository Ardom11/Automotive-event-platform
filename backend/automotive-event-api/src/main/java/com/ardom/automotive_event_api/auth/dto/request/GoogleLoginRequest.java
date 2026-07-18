package com.ardom.automotive_event_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for logging into account via Google OAuth")
public record GoogleLoginRequest(
        @Schema(description = "Google token needed to log in with.")
        @NotBlank
        String googleToken
) {
}
