package com.ardom.automotive_event_api.common.notification;

import com.ardom.automotive_event_api.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentData(
        BigDecimal amountPaid,
        LocalDateTime paidAt,
        PaymentStatus status
) {
}
