package com.ardom.automotive_event_api.event.dto;

import java.math.BigDecimal;

public record LocationDto(
        String place,
        String address,
        String city,
        String country,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
