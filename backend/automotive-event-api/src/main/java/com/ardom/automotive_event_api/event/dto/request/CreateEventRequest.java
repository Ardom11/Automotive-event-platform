package com.ardom.automotive_event_api.event.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateEventRequest(

        @NotBlank(message = "Name of event must not be blank")
        @Size(max = 100, message = "Name must be less than 100 symbols")
        String name,

        @NotBlank(message = "Description of event must not be blank")
        @Size(max = 5000, message = "Description must be less than 5000 symbols")
        String description,

        @NotBlank(message = "Place of event must not be blank")
        @Size(max = 100, message = "Place must be less than 100 symbols")
        String place,

        @NotBlank(message = "Address of event must not be blank")
        @Size(max = 100, message = "Address must be less than 100 symbols")
        String address,

        @NotBlank(message = "City of event must not be blank")
        @Size(max = 100, message = "City must be less than 100 symbols")
        String city,

        @NotBlank(message = "Country of event must not be blank")
        @Size(max = 100, message = "Country must be less than 100 symbols")
        String country,

        BigDecimal latitude,
        BigDecimal longitude,

        @NotNull(message = "Starting date of event must not be empty")
        @Future
        LocalDateTime dateStart,

        @NotNull(message = "Ending date of event must not be empty")
        @Future
        LocalDateTime dateEnd,

        @NotNull(message = "Tickets capacity of event must not be empty")
        @Positive
        Integer ticketsCapacity,

        @NotNull(message = "Ticket price of event must not be empty")
        @Positive
        BigDecimal ticketPrice,

        @NotNull(message = "Application fee of event must not be empty")
        @Positive
        BigDecimal applicationFee
) {
}
