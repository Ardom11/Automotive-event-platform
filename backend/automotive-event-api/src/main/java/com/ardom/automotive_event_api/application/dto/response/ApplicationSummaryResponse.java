package com.ardom.automotive_event_api.application.dto.response;

import com.ardom.automotive_event_api.application.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lightweight application summary for use in paginated lists")
public record ApplicationSummaryResponse(

        @Schema(description = "Unique application ID", example = "42")
        Long id,

        @Schema(description = "Current lifecycle status of the application", example = "DRAFT")
        ApplicationStatus status,

        @Schema(description = "Name of the event this application is for", example = "Milano AutoClassica 2026")
        String eventName,

        @Schema(description = "Summary of each car attached to this application, " +
                "each including only the first uploaded photo")
        List<CarSummaryResponse> cars
) {
}
