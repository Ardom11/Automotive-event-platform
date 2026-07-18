package com.ardom.automotive_event_api.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = """
        Request body for updating a DRAFT or UNPUBLISHED event.
        All fields are optional — only non-null values are applied.
        Numeric fields (ticketsCapacity, ticketPrice, applicationFee) are ignored if zero or negative.
        Date fields are ignored if they are not in the future.
        """)
public record UpdateEventRequest(

        @Schema(description = "New display name. Ignored if null.", example = "Milano AutoClassica 2026", maxLength = 100)
        @Size(max = 100, message = "Name must be less than 100 symbols")
        String name,

        @Schema(description = "New description. Ignored if null.", maxLength = 5000,
                example = "Updated description with new details about the event programme.")
        @Size(max = 5000, message = "Description must be less than 5000 symbols")
        String description,

        @Schema(description = "New venue name. Ignored if null.", example = "Autodromo Nazionale Monza", maxLength = 100)
        @Size(max = 100, message = "Place must be less than 100 symbols")
        String place,

        @Schema(description = "New street address. Ignored if null.", example = "Via Vedano, 5", maxLength = 100)
        @Size(max = 100, message = "Address must be less than 100 symbols")
        String address,

        @Schema(description = "New city. Ignored if null.", example = "Monza", maxLength = 100)
        @Size(max = 100, message = "City must be less than 100 symbols")
        String city,

        @Schema(description = "New country. Ignored if null.", example = "Italy", maxLength = 100)
        @Size(max = 100, message = "Country must be less than 100 symbols")
        String country,

        @Schema(description = "Updated GPS latitude. Ignored if null.", example = "45.6156")
        BigDecimal latitude,

        @Schema(description = "Updated GPS longitude. Ignored if null.", example = "9.2811")
        BigDecimal longitude,

        @Schema(description = "New start date and time. Ignored if null or not in the future.",
                example = "2026-06-14T10:00:00")
        @Future
        LocalDateTime dateStart,

        @Schema(description = "New end date and time. Ignored if null or not in the future. " +
                "Must not be before the (updated) start date.",
                example = "2026-06-14T18:00:00")
        @Future
        LocalDateTime dateEnd,

        @Schema(description = "New ticket capacity. Ignored if null or not positive.", example = "600")
        @Positive
        Integer ticketsCapacity,

        @Schema(description = "New ticket price. Ignored if null or not positive.", example = "30.00")
        @Positive
        BigDecimal ticketPrice,

        @Schema(description = "New per-car application fee. Ignored if null or not positive.", example = "80.00")
        @Positive
        BigDecimal applicationFee
) {
}
