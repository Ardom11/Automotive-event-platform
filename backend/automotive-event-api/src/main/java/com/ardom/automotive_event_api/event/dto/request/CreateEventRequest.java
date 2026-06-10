package com.ardom.automotive_event_api.event.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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

        @NotBlank(message = "Starting date of event must not be blank")
        @Future
        LocalDateTime dateStart,

        @NotBlank(message = "Ending date of event must not be blank")
        @Future
        LocalDateTime dateEnd,

        @NotBlank(message = "Tickets capacity of event must not be blank")
        @Positive
        Integer ticketsCapacity,

        @NotBlank(message = "Ticket price of event must not be blank")
        @Positive
        BigDecimal ticketPrice,

        @NotBlank(message = "Application fee of event must not be blank")
        @Positive
        BigDecimal applicationFee
) {
}
