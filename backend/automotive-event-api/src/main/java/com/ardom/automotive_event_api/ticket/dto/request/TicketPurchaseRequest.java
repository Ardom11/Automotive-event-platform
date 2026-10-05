package com.ardom.automotive_event_api.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request body for purchasing tickets as an authenticated user")
public record TicketPurchaseRequest(

        @Schema(description = "ID of the published event to purchase tickets for", example = "7")
        @NotNull
        Long eventId,

        @Schema(description = "Name of the attendee for each ticket being purchased. " +
                "List size determines quantity (1–20 tickets per purchase).")
        @NotEmpty
        @Size(max = 20)
        List<@Valid TicketHolderRequest> ticketHolders
) {
    public Integer quantity() {
        return ticketHolders.size();
    }
}
