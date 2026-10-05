package com.ardom.automotive_event_api.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "A single attendee's name for one ticket")
public record TicketHolderRequest(

        @Schema(example = "Marco")
        @NotBlank
        String name,

        @Schema(example = "Rossi")
        @NotBlank
        String surname
) {
}
