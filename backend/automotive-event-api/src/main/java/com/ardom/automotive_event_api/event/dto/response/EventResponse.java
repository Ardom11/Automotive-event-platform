package com.ardom.automotive_event_api.event.dto.response;

import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.dto.LocationDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String description,
        LocationDto location,
        LocalDateTime dateStart,
        LocalDateTime dateEnd,
        LocalDate applicationDeadline,
        LocalDate paymentDeadline,
        Integer ticketsCapacity,
        BigDecimal ticketPrice,
        BigDecimal applicationFee,
        EventStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
}
