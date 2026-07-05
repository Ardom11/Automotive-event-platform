package com.ardom.automotive_event_api.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationExpirationService {

    private final ApplicationRepository applicationRepository;

    @Transactional
    public void expireUnpaidApplications() {
        LocalDate today = LocalDate.now();

        List<Application> toExpire = applicationRepository
                .findAllByStatusAndEvent_DateStartBefore(
                        ApplicationStatus.APPROVED_WAITING_PAYMENT,
                        today.plusDays(7).atStartOfDay()
                );

        toExpire.forEach(app -> app.setStatus(ApplicationStatus.EXPIRED));
    }
}
