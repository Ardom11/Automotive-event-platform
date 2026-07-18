package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.event.dto.request.CreateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventStatusRequest;
import com.ardom.automotive_event_api.event.dto.response.AdminEventSummaryResponse;
import com.ardom.automotive_event_api.event.dto.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Tag(name = "Admin — Events", description = "Admin operations for creating and managing events")
@SecurityRequirement(name = "BearerAuth")
public class AdminEventController {

    private final EventService eventService;

    @Operation(
            summary = "Create a new event",
            description = "Creates a new event in `DRAFT` status. " +
                    "The event is not visible to the public until it is transitioned to `PUBLISHED`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created in DRAFT status",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body validation failed " +
                    "(blank fields, past dates, non-positive prices, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @Operation(
            summary = "List all events",
            description = "Returns a paginated list of all events regardless of status. " +
                    "Default page size is 10. Supports `page`, `size`, and `sort` query params."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list returned (may be empty)",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<AdminEventSummaryResponse>> getEvents(
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.getAllEvents(pageable));
    }

    @Operation(
            summary = "Get full event details",
            description = "Returns complete event data including deadlines, pricing, and audit timestamps. " +
                    "Works for events of any status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @Parameter(description = "ID of the event to retrieve", example = "7")
            @PathVariable Long id) {
        return ResponseEntity.ok(eventService.getFullEvent(id));
    }

    @Operation(
            summary = "Update event details",
            description = """
                    Partially updates an event. Only `DRAFT` and `UNPUBLISHED` events can be edited.
                    
                    **Field behaviour:**
                    - All fields are optional — only non-null values are applied
                    - `dateStart` / `dateEnd` are ignored if not in the future
                    - `ticketsCapacity`, `ticketPrice`, `applicationFee` are ignored if zero or negative
                    - `dateEnd` must not be before `dateStart` after both are resolved — returns 400 if violated
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = """
                    One of:
                    - Request body validation failed
                    - Resolved dateEnd is before dateStart
                    - Event is not in DRAFT or UNPUBLISHED status
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @Parameter(description = "ID of the event to update", example = "7")
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @Operation(
            summary = "Transition event status",
            description = """
                    Moves an event to a new lifecycle status.
                    
                    **Valid transitions:**
                    - `DRAFT` → `PUBLISHED`
                    - `PUBLISHED` → `UNPUBLISHED`, `ARCHIVED`
                    - `UNPUBLISHED` → `PUBLISHED`, `ARCHIVED`
                    - `ARCHIVED` → *(terminal — no further transitions)*
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requested status transition is not allowed from current status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateEventStatus(
            @Parameter(description = "ID of the event to transition", example = "7")
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventStatusRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEventStatus(id, request.status()));
    }

    @Operation(
            summary = "Delete an event",
            description = "Permanently deletes an event. Only `DRAFT` events can be deleted — " +
                    "any other status returns 409."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event deleted, no content returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Event cannot be deleted because it is not in DRAFT status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "ID of the event to delete", example = "7")
            @PathVariable Long id
    ) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
