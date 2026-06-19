package com.ardom.automotive_event_api.application.dto.response;

import com.ardom.automotive_event_api.application.ApplicationStatus;

import java.math.BigDecimal;
import java.util.List;

public record ApplicationResponse(
        Long id,
        String eventName,
        ApplicationStatus status,
        BigDecimal fee,
        List<CarResponse> cars,
        String rejectionReason
) {
}
