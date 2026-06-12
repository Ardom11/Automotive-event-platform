package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.request.CreateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventStatusRequest;
import com.ardom.automotive_event_api.event.dto.response.AdminEventSummaryResponse;
import com.ardom.automotive_event_api.event.dto.response.EventResponse;
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
public class AdminEventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @GetMapping
    public ResponseEntity<Page<AdminEventSummaryResponse>> getEvents(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(eventService.getAllEvents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getFullEvent(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateEventStatus(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateEventStatusRequest request) {
        return ResponseEntity.ok(eventService.updateEventStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
