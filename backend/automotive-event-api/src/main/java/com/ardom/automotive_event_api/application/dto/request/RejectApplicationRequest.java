package com.ardom.automotive_event_api.application.dto.request;

import jakarta.validation.constraints.Size;

public record RejectApplicationRequest(
        @Size(max = 200)
        String rejectionReason
) {
}
