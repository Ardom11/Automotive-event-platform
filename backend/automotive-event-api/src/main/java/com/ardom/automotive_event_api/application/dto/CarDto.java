package com.ardom.automotive_event_api.application.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record CarDto(
        @Size(max = 50)
        String brand,
        @Size(max = 50)
        String model,
        @Size(min = 1886)
        Short year,
        @Size(max = 1500)
        String story,
        @Size(min = 1, max = 20)
        List<String> photoKeys
) {
}
