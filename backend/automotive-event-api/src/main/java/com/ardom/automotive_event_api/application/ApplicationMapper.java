package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.response.*;
import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationMapper {
    private final UserMapper userMapper;

    public Application toBlankApplication(User user, Event event) {
        return Application.builder()
                .user(user)
                .event(event)
                .status(ApplicationStatus.DRAFT)
                .build();
    }

    public ApplicationResponse toResponse(Application application, List<CarResponse> cars) {
        return new ApplicationResponse(
                application.getId(),
                application.getEvent().getName(),
                application.getStatus(),
                application.getFee(),
                cars,
                application.getRejectionReason()
        );
    }

    public ApplicationSummaryResponse toSummaryResponse(Application application, List<CarSummaryResponse> cars){
        return new ApplicationSummaryResponse(
                application.getId(),
                application.getStatus(),
                application.getEvent().getName(),
                cars
        );
    }

    public AdminApplicationResponse toAdminResponse(Application application, List<CarResponse> cars) {
        return new AdminApplicationResponse(
                application.getId(),
                userMapper.toResponse(application.getUser()),
                application.getEvent().getName(),
                application.getStatus(),
                application.getFee(),
                cars,
                application.getRejectionReason(),
                application.getCreatedAt(),
                application.getModifiedAt()
        );
    }
}
