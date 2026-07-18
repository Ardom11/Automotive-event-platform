package com.ardom.automotive_event_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = """
        Request body for updating the authenticated user's profile.
        Both fields are optional — only non-null, non-blank values are applied.
        Sending both fields as null or blank is a no-op and returns null from the service.
        """)
public record UpdateProfileRequest(

        @Schema(description = "New first name. Ignored if null or blank.",
                example = "Marco", minLength = 2, maxLength = 50)
        @Size(min = 2, max = 50, message = "Name must be from 2 to 50 symbols")
        String name,

        @Schema(description = "New last name. Ignored if null or blank.",
                example = "Rossi", minLength = 2, maxLength = 50)
        @Size(min = 2, max = 50, message = "Surname must be from 2 to 50 symbols")
        String surname
) {
}
