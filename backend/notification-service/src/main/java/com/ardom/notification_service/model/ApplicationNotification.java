package com.ardom.notification_service.model;

import com.ardom.notification_service.model.enums.ApplicationEventType;

import java.time.LocalDate;

public record ApplicationNotification(
        Long applicationId,
        String applicantEmail,
        String eventName,
        LocalDate paymentDeadline,
        String rejectionReason,
        ApplicationEventType eventType
) {
}
