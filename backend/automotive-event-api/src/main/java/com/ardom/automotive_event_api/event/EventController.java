package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.event.dto.response.EventSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Public event browsing — no authentication required")
public class EventController {

    private final EventService eventService;

    @Operation(
            summary = "List all published events",
            description = "Returns a paginated list of events in `PUBLISHED` status. " +
                    "Accessible without authentication. Supports `page`, `size`, and `sort` query params."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list returned (may be empty)",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> getEvents(
            @Parameter(hidden = true)
            Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.getPublishedEvents(pageable));
    }

    @Operation(
            summary = "Get a published event",
            description = "Returns public summary of a single event. " +
                    "Returns 404 if the event does not exist or is not in `PUBLISHED` status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found",
                    content = @Content(schema = @Schema(implementation = EventSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found or not published",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventSummaryResponse> getEvent(
            @Parameter(description = "ID of the event to retrieve", example = "7")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.getEventSummary(id));
    }
}
