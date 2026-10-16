package com.ardom.notification_service.model;

import com.ardom.notification_service.model.enums.ApplicationStatus;

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
