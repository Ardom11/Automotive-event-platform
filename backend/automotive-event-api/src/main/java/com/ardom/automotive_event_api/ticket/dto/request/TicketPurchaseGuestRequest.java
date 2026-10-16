package com.ardom.automotive_event_api.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Request body for purchasing tickets as an unauthenticated guest. " +
        "Tickets and confirmation email are sent to the provided guest email address.")
public record TicketPurchaseGuestRequest(

        @Schema(description = "ID of the published event to purchase tickets for", example = "7")
        @NotNull
        Long eventId,

        @Schema(description = "Name of the attendee for each ticket being purchased.")
        @NotEmpty @Size(max = 20)
        List<@Valid TicketHolderRequest> ticketHolders,

        @Schema(description = "Guest's email address — tickets and confirmation are sent here",
                example = "marco.rossi@example.com")
        @Email
        @NotBlank
        String guestEmail
) {
    public int quantity() {
        return ticketHolders.size();
    }
}
