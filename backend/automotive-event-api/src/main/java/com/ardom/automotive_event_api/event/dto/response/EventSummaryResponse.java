package com.ardom.automotive_event_api.event.dto.response;

import com.ardom.automotive_event_api.event.dto.LocationDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Public-facing event summary shown to unauthenticated and regular users")
public record EventSummaryResponse(

        @Schema(description = "Display name of the event", example = "Milano AutoClassica 2026")
        String name,

        @Schema(description = "Short event description", example = "Annual classic car gathering in the heart of Milan.")
        String description,

        @Schema(description = "Location of the event venue")
        LocationDto location,

        @Schema(description = "Event start date and time (ISO-8601)", example = "2026-06-14T10:00:00")
        LocalDateTime dateStart,

        @Schema(description = "Event end date and time (ISO-8601)", example = "2026-06-14T18:00:00")
        LocalDateTime dateEnd,

        @Schema(description = "Price per spectator ticket", example = "25.00")
        BigDecimal ticketPrice
) {
}
