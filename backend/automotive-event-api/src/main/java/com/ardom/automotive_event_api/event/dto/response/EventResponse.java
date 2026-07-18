package com.ardom.automotive_event_api.event.dto.response;

import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.dto.LocationDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Full event details returned to admins, including deadlines and audit timestamps")
public record EventResponse(

        @Schema(description = "Unique event ID", example = "7")
        Long id,

        @Schema(description = "Display name of the event", example = "Milano AutoClassica 2026")
        String name,

        @Schema(description = "Full event description", example = "Annual classic car gathering in the heart of Milan.")
        String description,

        @Schema(description = "Full location details of the event venue")
        LocationDto location,

        @Schema(description = "Event start date and time (ISO-8601)", example = "2026-06-14T10:00:00")
        LocalDateTime dateStart,

        @Schema(description = "Event end date and time (ISO-8601)", example = "2026-06-14T18:00:00")
        LocalDateTime dateEnd,

        @Schema(description = "Deadline by which applications must be submitted. " +
                "Derived from event start date (typically 2 weeks before).",
                example = "2026-05-31")
        LocalDate applicationDeadline,

        @Schema(description = "Deadline by which approved applicants must complete payment. " +
                "Derived from application deadline.",
                example = "2026-06-07")
        LocalDate paymentDeadline,

        @Schema(description = "Maximum number of spectator tickets available", example = "500")
        Integer ticketsCapacity,

        @Schema(description = "Price per spectator ticket", example = "25.00")
        BigDecimal ticketPrice,

        @Schema(description = "Fee charged per car on an approved application", example = "75.00")
        BigDecimal applicationFee,

        @Schema(description = """
                Current lifecycle status:
                - `DRAFT` — not yet visible to the public
                - `PUBLISHED` — visible and open for applications/tickets
                - `UNPUBLISHED` — temporarily hidden; can be republished
                - `ARCHIVED` — permanently closed; no further transitions possible
                """,
                example = "PUBLISHED")
        EventStatus status,

        @Schema(description = "Timestamp when the event was created (UTC)", example = "2026-10-01T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp of the last modification (UTC)", example = "2026-11-15T14:30:00")
        LocalDateTime modifiedAt
) {
}
