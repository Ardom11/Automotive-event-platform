package com.ardom.automotive_event_api.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for rejecting a pending application")
public record RejectApplicationRequest(
        @Schema(description = "Optional reason for rejection, shown to the applicant. " +
                "If blank, no reason is stored.",
                example = "Vehicle does not meet the pre-1980 eligibility requirement.",
                maxLength = 200)
        @Size(max = 200)
        String rejectionReason
) {
}
