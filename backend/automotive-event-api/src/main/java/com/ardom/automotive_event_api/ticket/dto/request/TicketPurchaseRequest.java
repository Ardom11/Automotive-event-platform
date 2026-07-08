package com.ardom.automotive_event_api.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TicketPurchaseRequest(
        @NotNull
        Long eventId,

        @NotNull
        @Min(1)
        Integer quantity
) {
}
