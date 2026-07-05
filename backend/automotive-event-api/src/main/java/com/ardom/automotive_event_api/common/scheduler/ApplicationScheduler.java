package com.ardom.automotive_event_api.common.scheduler;

import com.ardom.automotive_event_api.application.ApplicationExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationScheduler {

    private final ApplicationExpirationService expirationService;

    @Scheduled(cron = "0 0 0 * * *")
    public void expireUnpaidApplications(){
        expirationService.expireUnpaidApplications();
    }
}
