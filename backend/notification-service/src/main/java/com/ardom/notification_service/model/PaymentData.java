package com.ardom.notification_service.model;

import com.ardom.notification_service.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentData(
        BigDecimal amountPaid,
        LocalDateTime paidAt,
        PaymentStatus status
) {
}
