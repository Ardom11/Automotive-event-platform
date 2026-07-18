package com.ardom.automotive_event_api.application.dto.response;

import com.ardom.automotive_event_api.application.ApplicationStatus;
import com.ardom.automotive_event_api.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Full application details as seen by an admin, including user info and audit timestamps")
public record AdminApplicationResponse(

        @Schema(description = "Unique application ID", example = "42")
        Long id,

        @Schema(description = "The user who submitted this application")
        UserResponse user,

        @Schema(description = "Name of the event this application is for", example = "Milano AutoClassica 2026")
        String eventName,

        @Schema(description = """
                Current lifecycle status of the application:
                - `DRAFT` — created, not yet submitted
                - `PENDING` — submitted, awaiting organiser review
                - `APPROVED_WAITING_PAYMENT` — approved, payment not yet completed
                - `REJECTED` — rejected by organiser; applicant can edit and resubmit
                - `COMPLETED` — approved and payment confirmed
                - `EXPIRED` — submission window closed before the application was submitted
                """,
                example = "PENDING")
        ApplicationStatus status,

        @Schema(description = "Total fee for this application (event fee × number of cars). " +
                "Null until the application is submitted.",
                example = "150.00")
        BigDecimal fee,

        @Schema(description = "All cars attached to this application, with full photo lists")
        List<CarResponse> cars,

        @Schema(description = "Rejection reason provided by the admin. Null unless status is `REJECTED`.",
                example = "Vehicle does not meet the pre-1980 eligibility requirement.")
        String rejectionReason,

        @Schema(description = "Timestamp when the application was first created (UTC)",
                example = "2026-11-03T14:22:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp of the last status or data change (UTC)",
                example = "2026-11-10T09:45:00")
        LocalDateTime modifiedAt
) {
}
