package com.ardom.automotive_event_api.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateApplicationRequest(
        @NotNull
        Long eventId
) {
}
