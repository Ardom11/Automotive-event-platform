package com.ardom.automotive_event_api.application.dto.response;

import java.util.List;

public record CarResponse(
        Long id,
        String brand,
        String model,
        Short year,
        String story,
        List<CarPhotoResponse> carPhotos
) {
}
