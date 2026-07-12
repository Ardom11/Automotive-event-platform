package com.ardom.automotive_event_api.common.scheduler;

import com.ardom.automotive_event_api.application.ApplicationExpirationService;
import com.ardom.automotive_event_api.application.payment.ApplicationPaymentExpirationService;
import com.ardom.automotive_event_api.ticket.payment.TicketPaymentExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler {

    private final ApplicationExpirationService applicationExpirationService;
    private final ApplicationPaymentExpirationService applicationPaymentExpirationService;
    private final TicketPaymentExpirationService ticketPaymentExpirationService;

    @Scheduled(cron = "0 0 0 * * *")
    public void expireUnpaidApplications(){
        applicationExpirationService.expireUnpaidApplications();
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void expirePayments(){
        applicationPaymentExpirationService.expireNotFinishedPayments();
        ticketPaymentExpirationService.expireNotFinishedPayments();
    }
}
