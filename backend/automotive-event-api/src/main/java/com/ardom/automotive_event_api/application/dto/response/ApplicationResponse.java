package com.ardom.automotive_event_api.application.dto.response;

import com.ardom.automotive_event_api.application.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Full details of an application, including all cars and their photos")
public record ApplicationResponse(

        @Schema(description = "Unique application ID", example = "42")
        Long id,

        @Schema(description = "Name of the event this application is for", example = "Milano AutoClassica 2026")
        String eventName,

        @Schema(description = """
                Current lifecycle status of the application:
                - `DRAFT` — created, not yet submitted
                - `PENDING` — submitted, awaiting organiser review
                - `APPROVED_WAITING_PAYMENT` — approved, payment not yet completed
                - `REJECTED` — rejected by organiser; can be edited and resubmitted
                - `COMPLETED` — approved and payment confirmed
                - `EXPIRED` — submission window closed before the application was submitted
                """,
                example = "PENDING")
        ApplicationStatus status,

        @Schema(description = "Total fee charged for this application. " +
                "Calculated as event fee × number of cars. Null until the application is submitted.",
                example = "150.00")
        BigDecimal fee,

        @Schema(description = "All cars attached to this application")
        List<CarResponse> cars,

        @Schema(description = "Reason provided by the organiser when rejecting the application. " +
                "Null unless status is `REJECTED`.",
                example = "Vehicle does not meet the pre-1980 eligibility requirement.")
        String rejectionReason
) {
}
