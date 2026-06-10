package com.ardom.automotive_event_api.event.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateEventRequest(

        @Size(max = 100, message = "Name must be less than 100 symbols")
        String name,

        @Size(max = 5000, message = "Description must be less than 5000 symbols")
        String description,

        @Size(max = 100, message = "Place must be less than 100 symbols")
        String place,

        @Size(max = 100, message = "Address must be less than 100 symbols")
        String address,

        @Size(max = 100, message = "City must be less than 100 symbols")
        String city,

        @Size(max = 100, message = "Country must be less than 100 symbols")
        String country,

        BigDecimal latitude,
        BigDecimal longitude,

        @Future
        LocalDateTime dateStart,

        @Future
        LocalDateTime dateEnd,

        @Positive
        Integer ticketsCapacity,

        @Positive
        BigDecimal ticketPrice,

        @Positive
        BigDecimal applicationFee
) {
}
