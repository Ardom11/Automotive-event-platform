package com.ardom.automotive_event_api.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for creating a new application for an event")
public record CreateApplicationRequest(
        @Schema(description = "ID of the published event to apply for", example = "7")
        @NotNull
        Long eventId
) {
}
