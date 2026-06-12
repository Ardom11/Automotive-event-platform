package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.request.CreateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventRequest;
import com.ardom.automotive_event_api.event.dto.response.AdminEventSummaryResponse;
import com.ardom.automotive_event_api.event.dto.response.EventResponse;
import com.ardom.automotive_event_api.event.dto.response.EventSummaryResponse;
import com.ardom.automotive_event_api.event.exception.DeletingNotDraftEventException;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.InvalidStatusTransitionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = eventMapper.toEvent(request);

        event = eventRepository.save(event);

        return eventMapper.toAdminResponse(event);
    }

    @Transactional
    public EventResponse updateEventStatus(Long id, EventStatus status) {
        Event event = getEventById(id);

        isTransitionValid(event.getStatus(), status);
        event.setStatus(status);

        return eventMapper.toAdminResponse(eventRepository.save(event));

    }

    private boolean isTransitionValid(EventStatus current, EventStatus requested) {
        Map<EventStatus, Set<EventStatus>> allowed = Map.of(
                EventStatus.DRAFT, Set.of(EventStatus.PUBLISHED),
                EventStatus.PUBLISHED, Set.of(EventStatus.UNPUBLISHED, EventStatus.ARCHIVED),
                EventStatus.UNPUBLISHED, Set.of(EventStatus.PUBLISHED, EventStatus.ARCHIVED),
                EventStatus.ARCHIVED, Set.of()
        );

        if (allowed.get(current).contains(requested)) {
            return true;
        } else {
            throw new InvalidStatusTransitionException("Cannot change status from " + current + " to " + requested);
        }
    }

    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = getEventById(id);

        if (request.name() != null) {
            event.setName(request.name());
        }

        if (request.description() != null) {
            event.setDescription(request.description());
        }

        EventLocation location = event.getLocation();

        if (request.place() != null) {
            location.setPlace(request.place());
        }

        if (request.address() != null) {
            location.setAddress(request.address());
        }

        if (request.city() != null) {
            location.setCity(request.city());
        }

        if (request.country() != null) {
            location.setCountry(request.country());
        }

        if (request.latitude() != null) {
            location.setLatitude(request.latitude());
        }

        if (request.longitude() != null) {
            location.setLongitude(request.longitude());
        }

        event.setLocation(location);

        if (request.dateStart() != null && request.dateStart().isAfter(LocalDateTime.now())) {
            event.setDateStart(request.dateStart());
        }

        if (request.dateEnd() != null && request.dateEnd().isAfter(LocalDateTime.now())) {
            event.setDateEnd(request.dateEnd());
        }

        if (event.getDateEnd().isBefore(event.getDateStart())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (request.ticketsCapacity() != null && request.ticketsCapacity() > 0) {
            event.setTicketsCapacity(request.ticketsCapacity());
        }

        if (request.ticketPrice() != null && request.ticketPrice().compareTo(BigDecimal.valueOf(0)) > 0) {
            event.setTicketPrice(request.ticketPrice());
        }

        if (request.applicationFee() != null && request.applicationFee().compareTo(BigDecimal.valueOf(0)) > 0) {
            event.setApplicationFee(request.applicationFee());
        }

        return eventMapper.toAdminResponse(eventRepository.save(event));
    }

    public Page<AdminEventSummaryResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(eventMapper::toAdminSummaryResponse);
    }

    public Page<EventSummaryResponse> getPublishedEvents(Pageable pageable) {
        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)
                .map(eventMapper::toPublicResponse);
    }

    public EventResponse getFullEvent(Long id) {
        Event event = getEventById(id);

        return eventMapper.toAdminResponse(event);
    }

    public EventSummaryResponse getEventSummary(Long id) {
        Event event = eventRepository.findByIdAndStatus(id, EventStatus.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException("Event is not found"));

        return eventMapper.toPublicResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = getEventById(id);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new DeletingNotDraftEventException("Cannot delete event with status " + event.getStatus());
        }

        eventRepository.deleteById(id);
    }

    private Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + id + " is not found"));
    }
}
