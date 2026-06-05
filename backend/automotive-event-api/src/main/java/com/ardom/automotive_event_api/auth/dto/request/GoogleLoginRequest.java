package com.ardom.automotive_event_api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank
        String googleToken
) {
}
