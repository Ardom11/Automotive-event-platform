package com.ardom.automotive_event_api.common.notification;

import com.ardom.automotive_event_api.application.ApplicationStatus;

import java.util.List;

public record ApplicationPaymentNotification(
        Long applicationId,
        ApplicationStatus status,
        String applicantEmail,
        String applicantName,
        String applicantSurname,
        String eventName,
        List<CarData> cars,
        PaymentData paymentData
) {
}
