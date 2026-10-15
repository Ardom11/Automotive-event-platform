package com.ardom.automotive_event_api.common.notification;

import com.ardom.automotive_event_api.common.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final KafkaProducerService kafkaProducerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationNotification(ApplicationNotification e) {
        kafkaProducerService.sendApplicationNotification(e);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationPaymentNotification(ApplicationPaymentNotification e) {
        kafkaProducerService.sendApplicationPaymentNotification(e);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketNotification(TicketNotification e) {
        kafkaProducerService.sendTicketNotification(e);
    }
}
