package com.ardom.automotive_event_api.common.notification;

import java.time.LocalDate;

public record ApplicationNotification(
        Long applicationId,
        String applicantEmail,
        String eventName,
        LocalDate paymentDeadline,
        String rejectionReason
) {
}
