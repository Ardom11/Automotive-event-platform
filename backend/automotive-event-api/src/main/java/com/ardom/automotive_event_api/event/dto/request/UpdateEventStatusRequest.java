package com.ardom.automotive_event_api.event.dto.request;

import com.ardom.automotive_event_api.event.EventStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateEventStatusRequest(
        @NotBlank
        EventStatus status
) {
}
