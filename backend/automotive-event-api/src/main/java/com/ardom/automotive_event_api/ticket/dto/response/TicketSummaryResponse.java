package com.ardom.automotive_event_api.ticket.dto.response;

import com.ardom.automotive_event_api.ticket.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Lightweight ticket summary for paginated list views")
public record TicketSummaryResponse(

        @Schema(description = "Name of the event this ticket is for", example = "Milano AutoClassica 2026")
        String eventName,

        @Schema(description = "Human-readable event date string", example = "14 June 2026")
        String date,

        @Schema(description = "Unique ticket code used for entry validation at the event",
                example = "TKT-A3F9-2B7C")
        String code,

        @Schema(description = """
                Current status of the ticket:
                - `ACTIVE` — valid and not yet scanned at the event
                - `USED` — scanned and marked as used at entry
                """,
                example = "ACTIVE")
        TicketStatus status,

        @Schema(description = "Price paid for this individual ticket", example = "25.00")
        BigDecimal price,

        @Schema(description = "Unique ticket ID", example = "201")
        Long id
) {
}
