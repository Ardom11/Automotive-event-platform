package com.ardom.automotive_event_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for logging into account by email + password")
public record LoginRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        String password
) {
}
