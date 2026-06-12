package com.ardom.automotive_event_api.event.dto.response;

import com.ardom.automotive_event_api.event.EventStatus;

import java.time.LocalDateTime;

public record AdminEventSummaryResponse(
        Long id,
        String name,
        String city,
        LocalDateTime dateStart,
        LocalDateTime dateEnd,
        EventStatus status
) {
}
