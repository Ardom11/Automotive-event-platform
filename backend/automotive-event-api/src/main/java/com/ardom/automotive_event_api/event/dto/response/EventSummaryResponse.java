package com.ardom.automotive_event_api.event.dto.response;

import com.ardom.automotive_event_api.event.dto.LocationDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventSummaryResponse(
        String name,
        String description,
        LocationDto location,
        LocalDateTime dateStart,
        LocalDateTime dateEnd,
        BigDecimal ticketPrice
) {
}
