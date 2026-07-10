package com.ardom.automotive_event_api.ticket.util;

import java.util.UUID;

public final class TicketCodeGenerator {
    public static String generate() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }
}
