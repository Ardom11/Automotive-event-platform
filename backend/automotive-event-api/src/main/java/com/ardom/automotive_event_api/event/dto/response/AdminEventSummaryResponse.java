package com.ardom.automotive_event_api.event.dto.response;

import com.ardom.automotive_event_api.event.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Lightweight event summary for admin list views")
public record AdminEventSummaryResponse(

        @Schema(description = "Unique event ID", example = "7")
        Long id,

        @Schema(description = "Display name of the event", example = "Milano AutoClassica 2026")
        String name,

        @Schema(description = "City where the event takes place", example = "Monza")
        String city,

        @Schema(description = "Event start date and time", example = "2026-06-14T10:00:00")
        LocalDateTime dateStart,

        @Schema(description = "Event end date and time", example = "2026-06-14T18:00:00")
        LocalDateTime dateEnd,

        @Schema(description = "Current lifecycle status of the event", example = "PUBLISHED")
        EventStatus status
) {
}
