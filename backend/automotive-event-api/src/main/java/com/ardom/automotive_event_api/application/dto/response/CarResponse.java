package com.ardom.automotive_event_api.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Full car details as stored in an application")
public record CarResponse(

        @Schema(description = "Unique car ID", example = "101")
        Long id,

        @Schema(description = "Car manufacturer", example = "Porsche")
        String brand,

        @Schema(description = "Car model name", example = "911 Carrera")
        String model,

        @Schema(description = "Manufacturing year", example = "2003")
        Short year,

        @Schema(description = "Owner's story or description of the car",
                example = "Fully restored air-cooled 911, matching numbers, original Sepia Brown.")
        String story,

        @Schema(description = "All photos attached to this car, in upload order")
        List<CarPhotoResponse> carPhotos
) {
}
