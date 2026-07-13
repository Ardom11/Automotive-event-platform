package com.ardom.automotive_event_api.ticket;

import com.ardom.automotive_event_api.ticket.dto.response.TicketSummaryResponse;
import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<Page<TicketSummaryResponse>> getAllUserTickets(Authentication authentication,
                                                                         Pageable pageable) {
        return ResponseEntity.ok(ticketService.getUserTickets(authentication, pageable));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTicketPdf(Authentication authentication,
                                               @PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket.pdf")
                .body(ticketService.getTicketPdf(authentication, id));
    }
}
