package com.ardom.automotive_event_api.ticket;

import com.ardom.automotive_event_api.common.pdf.PdfService;
import com.ardom.automotive_event_api.ticket.dto.response.TicketSummaryResponse;
import com.ardom.automotive_event_api.ticket.exception.TicketNotFoundException;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final PdfService pdfService;

    public Page<TicketSummaryResponse> getUserTickets(Authentication authentication, Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        Page<Ticket> tickets = ticketRepository.findAllByUserId(user.getId(), pageable);

        return tickets.map(ticketMapper::toResponse);
    }

    public byte[] getTicketPdf(Authentication authentication, Long id) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        Ticket ticket = ticketRepository.findByIdAndUserId(id, user.getId());
        if (ticket == null) {
            throw new TicketNotFoundException("Ticket with id " + id + " is not found");
        }

        return pdfService.generateTicketPdf(ticket);
    }
}
