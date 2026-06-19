package com.ardom.automotive_event_api.application.dto.response;

import com.ardom.automotive_event_api.application.ApplicationStatus;
import com.ardom.automotive_event_api.user.dto.response.UserResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminApplicationResponse(
        Long id,
        UserResponse user,
        String eventName,
        ApplicationStatus status,
        BigDecimal fee,
        List<CarResponse> cars,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
}
