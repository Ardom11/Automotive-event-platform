package com.ardom.automotive_event_api.event.dto.request;

import com.ardom.automotive_event_api.event.EventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventStatusRequest(
        @NotNull
        EventStatus status
) {
}
