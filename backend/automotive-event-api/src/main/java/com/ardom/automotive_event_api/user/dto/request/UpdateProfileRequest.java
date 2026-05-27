package com.ardom.automotive_event_api.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 50, message = "Name must be from 2 to 50 symbols")
        String name,

        @Size(min = 2, max = 50, message = "Surname must be from 2 to 50 symbols")
        String surname
) {
}
