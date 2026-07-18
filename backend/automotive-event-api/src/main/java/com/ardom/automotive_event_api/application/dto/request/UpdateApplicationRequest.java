package com.ardom.automotive_event_api.application.dto.request;

import com.ardom.automotive_event_api.application.dto.CarDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request body for adding cars to an existing application. " +
        "Each call appends cars — existing cars are never removed.")
public record UpdateApplicationRequest(
        @Schema(description = "List of cars to add. Combined with existing cars must not exceed the maximum per application.",
                minLength = 1, maxLength = 5)
        @NotNull
        @Size(min = 1, max = 5)
        List<CarDto> cars
) {
}
