package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.response.EventSummaryResponse;
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
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> getEvents(Pageable pageable) {
        return ResponseEntity.ok(eventService.getPublishedEvents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventSummaryResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventSummary(id));
    }
}
