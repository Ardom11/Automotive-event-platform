package com.ardom.automotive_event_api.application.dto.request;

import com.ardom.automotive_event_api.application.dto.CarDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateApplicationRequest(
        @NotNull
        @Size(min = 1, max = 5)
        List<CarDto> cars
) {
}
