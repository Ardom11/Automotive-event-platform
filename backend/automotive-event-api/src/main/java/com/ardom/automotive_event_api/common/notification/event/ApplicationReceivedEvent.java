package com.ardom.automotive_event_api.common.notification.event;

import com.ardom.automotive_event_api.common.notification.ApplicationNotification;

public record ApplicationReceivedEvent(
        ApplicationNotification notification
) {
}
