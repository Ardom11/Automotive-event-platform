package com.ardom.automotive_event_api.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight car summary used in application list views")
public record CarSummaryResponse(

        @Schema(description = "Unique car ID", example = "101")
        Long id,

        @Schema(description = "Car manufacturer", example = "Porsche")
        String brand,

        @Schema(description = "Car model name", example = "911 Carrera")
        String model,

        @Schema(description = "Manufacturing year", example = "2003")
        Short year,

        @Schema(description = "First uploaded photo for this car. Null if no photos have been attached yet.")
        CarPhotoResponse carPhoto
) {
}
