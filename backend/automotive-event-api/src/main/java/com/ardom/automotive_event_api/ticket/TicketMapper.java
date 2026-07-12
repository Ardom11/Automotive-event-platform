package com.ardom.automotive_event_api.ticket;

import com.ardom.automotive_event_api.ticket.dto.response.TicketSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TicketMapper {
    public TicketSummaryResponse toResponse(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getEvent().getName(),
                String.format("%s - %s",
                        ticket.getEvent().getDateStart().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                        ticket.getEvent().getDateEnd().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))),
                ticket.getCode(),
                ticket.getStatus(),
                ticket.getPrice(),
                ticket.getId()
        );
    }
}
