package com.ardom.automotive_event_api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CarDto(

        @Schema(description = "Car manufacturer", example = "Porsche", maxLength = 50)
        @Size(max = 50)
        String brand,

        @Schema(description = "Car model name", example = "911 Carrera", maxLength = 50)
        @Size(max = 50)
        String model,

        @Schema(description = "Manufacturing year. Must be 1886 or later (year of the first automobile).",
                example = "2003", minimum = "1886")
        @Min(1886)
        Short year,

        @Schema(description = "Owner's story or description of the car. Shown on the event profile.",
                example = "Fully restored air-cooled 911, matching numbers, original Sepia Brown.",
                maxLength = 1500)
        @Size(max = 1500)
        String story,

        @Schema(description = "S3 photo keys previously uploaded via the storage endpoint. " +
                "Keys must belong to the authenticated user (format: `<prefix>/<userId>/filename`).",
                example = "[\"photos/42/porsche-front.jpg\", \"photos/42/porsche-rear.jpg\"]",
                minLength = 1, maxLength = 20)
        @Size(min = 1, max = 20)
        List<String> photoKeys
) {
}
