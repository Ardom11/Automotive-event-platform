package com.ardom.automotive_event_api.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for purchasing tickets as an authenticated user")
public record TicketPurchaseRequest(

        @Schema(description = "ID of the published event to purchase tickets for", example = "7")
        @NotNull
        Long eventId,

        @Schema(description = "Number of tickets to purchase. Must be at least 1 and must not exceed remaining capacity.",
                example = "2", minimum = "1")
        @NotNull
        @Min(1)
        Integer quantity
) {
}
