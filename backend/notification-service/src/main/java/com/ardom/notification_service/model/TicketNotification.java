package com.ardom.notification_service.model;

import java.util.List;

public record TicketNotification(
        String recipientEmail,
        List<TicketData> tickets
) {
}
