package com.ardom.automotive_event_api.common.notification.event;

import com.ardom.automotive_event_api.common.notification.ApplicationPaymentNotification;

public record ApplicationPaidEvent(
        ApplicationPaymentNotification notification
) {
}
