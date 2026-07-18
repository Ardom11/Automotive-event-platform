package com.ardom.automotive_event_api.user.dto.response;

import com.ardom.automotive_event_api.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic user profile information")
public record UserResponse(

        @Schema(description = "Unique user ID", example = "15")
        Long id,

        @Schema(description = "User's first name", example = "Marco")
        String name,

        @Schema(description = "User's last name", example = "Rossi")
        String surname,

        @Schema(description = "User's email address", example = "marco.rossi@example.com")
        String email,

        @Schema(description = "User's role in the system", example = "USER")
        Role role
) {
}
