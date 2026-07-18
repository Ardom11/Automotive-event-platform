package com.ardom.automotive_event_api.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for registering a new account")
public record RegisterRequest(

        @Schema(minLength = 2, maxLength = 50)
        @NotBlank
        @Size(min = 2, max = 50)
        String name,

        @Schema(minLength = 2, maxLength = 50)
        @NotBlank
        @Size(min = 2, max = 50)
        String surname,

        @Email
        @NotBlank
        String email,

        @Schema(minLength = 4)
        @NotBlank
        @Size(min = 4)
        String password
) {
}
