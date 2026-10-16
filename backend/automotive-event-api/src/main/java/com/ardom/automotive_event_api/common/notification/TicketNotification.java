package com.ardom.automotive_event_api.common.notification;

import java.util.List;

public record TicketNotification(
        String recipientEmail,
        List<TicketData> tickets
) {
}
