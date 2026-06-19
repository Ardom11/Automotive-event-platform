package com.ardom.automotive_event_api.application.dto.response;

public record CarSummaryResponse(
        Long id,
        String brand,
        String model,
        Short year,
        CarPhotoResponse carPhoto
) {
}
