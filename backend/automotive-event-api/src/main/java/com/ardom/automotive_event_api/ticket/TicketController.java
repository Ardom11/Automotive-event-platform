package com.ardom.automotive_event_api.ticket;

import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.ticket.dto.response.TicketSummaryResponse;
import com.google.common.net.HttpHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tickets", description = "View and download tickets for the authenticated user")
@SecurityRequirement(name = "BearerAuth")
public class TicketController {

    private final TicketService ticketService;

    @Operation(
            summary = "List all tickets for the current user",
            description = "Returns a paginated list of ticket summaries belonging to the authenticated user. " +
                    "Includes both ACTIVE and USED tickets. Supports `page`, `size`, and `sort` query params."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list returned (may be empty)",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<TicketSummaryResponse>> getAllUserTickets(
            Authentication authentication,
            @Parameter(hidden = true) Pageable pageable
    ) {
        return ResponseEntity.ok(ticketService.getUserTickets(authentication, pageable));
    }

    @Operation(
            summary = "Download ticket as PDF",
            description = "Generates and returns a PDF file for a single ticket. " +
                    "Only the ticket owner can download it. " +
                    "The response is a binary PDF attachment named `ticket.pdf`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                    content = @Content(mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Ticket not found or belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTicketPdf(
            Authentication authentication,
            @Parameter(description = "ID of the ticket to download", example = "201")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket.pdf")
                .body(ticketService.getTicketPdf(authentication, id));
    }
}
