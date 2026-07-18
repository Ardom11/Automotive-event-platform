package com.ardom.automotive_event_api.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request body for creating a new event. Created events start in DRAFT status.")
public record CreateEventRequest(

        @Schema(description = "Display name of the event", example = "Milano AutoClassica 2026", maxLength = 100)
        @NotBlank(message = "Name of event must not be blank")
        @Size(max = 100, message = "Name must be less than 100 symbols")
        String name,

        @Schema(description = "Full description of the event shown to visitors", maxLength = 5000,
                example = "Annual classic car gathering in the heart of Milan, featuring pre-1980 vehicles from across Europe.")
        @NotBlank(message = "Description of event must not be blank")
        @Size(max = 5000, message = "Description must be less than 5000 symbols")
        String description,

        @Schema(description = "Venue or place name", example = "Autodromo Nazionale Monza", maxLength = 100)
        @NotBlank(message = "Place of event must not be blank")
        @Size(max = 100, message = "Place must be less than 100 symbols")
        String place,

        @Schema(description = "Street address of the venue", example = "Via Vedano, 5", maxLength = 100)
        @NotBlank(message = "Address of event must not be blank")
        @Size(max = 100, message = "Address must be less than 100 symbols")
        String address,

        @Schema(description = "City where the event takes place", example = "Monza", maxLength = 100)
        @NotBlank(message = "City of event must not be blank")
        @Size(max = 100, message = "City must be less than 100 symbols")
        String city,

        @Schema(description = "Country where the event takes place", example = "Italy", maxLength = 100)
        @NotBlank(message = "Country of event must not be blank")
        @Size(max = 100, message = "Country must be less than 100 symbols")
        String country,

        @Schema(description = "GPS latitude coordinate of the venue. Optional but recommended for map display.",
                example = "45.6156")
        BigDecimal latitude,

        @Schema(description = "GPS longitude coordinate of the venue. Optional but recommended for map display.",
                example = "9.2811")
        BigDecimal longitude,

        @Schema(description = "Event start date and time (ISO-8601, must be in the future)",
                example = "2026-06-14T10:00:00")
        @NotNull(message = "Starting date of event must not be empty")
        @Future
        LocalDateTime dateStart,

        @Schema(description = "Event end date and time (ISO-8601, must be in the future and after dateStart)",
                example = "2026-06-14T18:00:00")
        @NotNull(message = "Ending date of event must not be empty")
        @Future
        LocalDateTime dateEnd,

        @Schema(description = "Maximum number of spectator tickets available", example = "500")
        @NotNull(message = "Tickets capacity of event must not be empty")
        @Positive
        Integer ticketsCapacity,

        @Schema(description = "Price per spectator ticket in the platform currency", example = "25.00")
        @NotNull(message = "Ticket price of event must not be empty")
        @Positive
        BigDecimal ticketPrice,

        @Schema(description = "Fee charged per car on an approved application. " +
                "Total application fee = applicationFee × numberOfCars.", example = "75.00")
        @NotNull(message = "Application fee of event must not be empty")
        @Positive
        BigDecimal applicationFee
) {
}
