package com.ardom.automotive_event_api.application.dto.response;

import com.ardom.automotive_event_api.application.ApplicationStatus;

import java.util.List;

public record ApplicationSummaryResponse(
        Long id,
        ApplicationStatus status,
        String eventName,
        List<CarSummaryResponse> cars
) {
}
