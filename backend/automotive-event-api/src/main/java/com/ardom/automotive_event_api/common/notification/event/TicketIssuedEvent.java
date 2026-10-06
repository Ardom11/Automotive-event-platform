package com.ardom.automotive_event_api.common.notification.event;

import com.ardom.automotive_event_api.common.notification.TicketNotification;

public record TicketIssuedEvent(
        TicketNotification notification
) {
}
