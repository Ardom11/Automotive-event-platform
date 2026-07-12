package com.ardom.automotive_event_api.ticket.dto.response;

import com.ardom.automotive_event_api.ticket.TicketStatus;

import java.math.BigDecimal;

public record TicketSummaryResponse(
        String eventName,
        String date,
        String code,
        TicketStatus status,
        BigDecimal price,
        Long id
) {
}
