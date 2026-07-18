package com.ardom.automotive_event_api.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Physical location details of an event")
public record LocationDto(

        @Schema(description = "Venue or place name", example = "Autodromo Nazionale Monza")
        String place,

        @Schema(description = "Street address", example = "Via Vedano, 5")
        String address,

        @Schema(description = "City", example = "Monza")
        String city,

        @Schema(description = "Country", example = "Italy")
        String country,

        @Schema(description = "GPS latitude coordinate", example = "45.6156")
        BigDecimal latitude,

        @Schema(description = "GPS longitude coordinate", example = "9.2811")
        BigDecimal longitude
) {
}
