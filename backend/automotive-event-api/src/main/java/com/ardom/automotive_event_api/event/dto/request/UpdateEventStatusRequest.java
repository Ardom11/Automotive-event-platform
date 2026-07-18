package com.ardom.automotive_event_api.event.dto.request;

import com.ardom.automotive_event_api.event.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for transitioning an event to a new status")
public record UpdateEventStatusRequest(
        @Schema(description = """
                Target status. Valid transitions:
                - `DRAFT` → `PUBLISHED`
                - `PUBLISHED` → `UNPUBLISHED`, `ARCHIVED`
                - `UNPUBLISHED` → `PUBLISHED`, `ARCHIVED`
                - `ARCHIVED` → *(no transitions allowed)*
                """,
                example = "PUBLISHED")
        @NotNull
        EventStatus status
) {
}
