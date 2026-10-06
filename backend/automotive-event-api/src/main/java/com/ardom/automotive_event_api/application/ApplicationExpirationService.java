package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.common.notification.ApplicationNotification;
import com.ardom.automotive_event_api.common.notification.event.ApplicationExpiredEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationExpirationService {
    @Value("${application.expiration.unpaid-expiration-days-before-event}")
    private Integer daysBeforeEvent;

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void expireUnpaidApplications() {
        LocalDate today = LocalDate.now();

        List<Application> toExpire = applicationRepository
                .findAllByStatusAndEvent_DateStartBefore(
                        ApplicationStatus.APPROVED_WAITING_PAYMENT,
                        today.plusDays(daysBeforeEvent).atStartOfDay()
                );

        toExpire.forEach(app -> {
            app.setStatus(ApplicationStatus.EXPIRED);
            app = applicationRepository.save(app);
            eventPublisher.publishEvent(new ApplicationExpiredEvent(new ApplicationNotification(
                    app.getId(),
                    app.getUser().getEmail(),
                    app.getEvent().getName(),
                    app.getEvent().getPaymentDeadline(),
                    app.getRejectionReason()
            )));
        });
    }
}
