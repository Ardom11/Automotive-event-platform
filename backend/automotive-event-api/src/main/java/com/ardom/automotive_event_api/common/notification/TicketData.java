package com.ardom.automotive_event_api.common.notification;

import java.time.LocalDateTime;

public record TicketData(
        String ticketCode,
        String holderName,
        String holderSurname,
        String eventName,
        LocalDateTime eventDateStart,
        LocalDateTime eventDateEnd,
        String locationPlace,
        String locationCity,
        String locationCountry,
        String locationAddress
) {
}
