package com.ardom.automotive_event_api.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for purchasing tickets as an unauthenticated guest. " +
        "Tickets and confirmation email are sent to the provided guest email address.")
public record TicketPurchaseGuestRequest(

        @Schema(description = "ID of the published event to purchase tickets for", example = "7")
        @NotNull
        Long eventId,

        @Schema(description = "Number of tickets to purchase. Must be at least 1 and must not exceed remaining capacity.",
                example = "2", minimum = "1")
        @NotNull
        @Min(1)
        Integer quantity,

        @Schema(description = "Guest's first name", example = "Marco")
        @NotBlank
        String guestName,

        @Schema(description = "Guest's last name", example = "Rossi")
        @NotBlank
        String guestSurname,

        @Schema(description = "Guest's email address — tickets and confirmation are sent here",
                example = "marco.rossi@example.com")
        @Email
        @NotBlank
        String guestEmail
) {
}
