package com.ardom.automotive_event_api.common.notification;

import com.ardom.automotive_event_api.common.email.EmailService;
import com.ardom.automotive_event_api.common.notification.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationReceivedEvent e) {
        emailService.sendApplicationReceived(e.notification());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationApprovedEvent e) {
        emailService.sendApplicationApproved(e.notification());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationRejectedEvent e) {
        emailService.sendApplicationRejected(e.notification());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationExpiredEvent e) {
        emailService.sendApplicationExpired(e.notification());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ApplicationPaidEvent e) {
        emailService.sendPaymentConfirmed(e.notification());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TicketIssuedEvent e) {
        emailService.sendTicket(e.notification());
    }
}
