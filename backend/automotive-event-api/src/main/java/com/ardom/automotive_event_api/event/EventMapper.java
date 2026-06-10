package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.LocationDto;
import com.ardom.automotive_event_api.event.dto.request.CreateEventRequest;
import com.ardom.automotive_event_api.event.dto.response.EventResponse;
import com.ardom.automotive_event_api.event.dto.response.EventSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {
    public Event toEvent(CreateEventRequest request) {
        return Event.builder()
                .name(request.name())
                .description(request.description())
                .location(new EventLocation(
                        request.place(),
                        request.address(),
                        request.city(),
                        request.country(),
                        request.latitude(),
                        request.longitude())
                )
                .dateStart(request.dateStart())
                .dateEnd(request.dateEnd())
                .ticketsCapacity(request.ticketsCapacity())
                .ticketPrice(request.ticketPrice())
                .applicationFee(request.applicationFee())
                .status(EventStatus.DRAFT)
                .build();
    }

    public EventResponse toAdminResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                toLocationDto(event.getLocation()),
                event.getDateStart(),
                event.getDateEnd(),
                event.getApplicationDeadline(),
                event.getPaymentDeadline(),
                event.getTicketsCapacity(),
                event.getTicketPrice(),
                event.getApplicationFee(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getModifiedAt()
        );
    }

    public EventSummaryResponse toPublicResponse(Event event) {
        return new EventSummaryResponse(
                event.getName(),
                event.getDescription(),
                toLocationDto(event.getLocation()),
                event.getDateStart(),
                event.getDateEnd(),
                event.getTicketPrice()
        );
    }

    public LocationDto toLocationDto(EventLocation location) {
        return new LocationDto(
                location.getPlace(),
                location.getAddress(),
                location.getCity(),
                location.getCountry(),
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
