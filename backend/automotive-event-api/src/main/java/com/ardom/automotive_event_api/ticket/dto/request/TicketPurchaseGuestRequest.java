package com.ardom.automotive_event_api.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketPurchaseGuestRequest(
        @NotNull
        Long eventId,

        @NotNull
        @Min(1)
        Integer quantity,

        @NotBlank
        String guestName,

        @NotBlank
        String guestSurname,

        @NotBlank
        String guestEmail
) {
}
