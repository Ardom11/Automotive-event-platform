package com.ardom.notification_service.consumer;

import com.ardom.notification_service.model.ApplicationNotification;
import com.ardom.notification_service.model.ApplicationPaymentNotification;
import com.ardom.notification_service.model.TicketNotification;
import com.ardom.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailService emailService;

    @KafkaListener(
            topics = "application-events",
            groupId = "notification-service-group",
            containerFactory = "applicationEventListenerFactory")
    public void consumeApplicationEvent(ConsumerRecord<String, Object> record) {
        Object payload = record.value();
        log.info("Received application event with key [{}]", record.key());

        if (payload instanceof ApplicationNotification notification) {
            switch (notification.eventType()) {
                case APPLICATION_RECEIVED -> emailService.sendApplicationReceived(notification);
                case APPLICATION_APPROVED -> emailService.sendApplicationApproved(notification);
                case APPLICATION_REJECTED -> emailService.sendApplicationRejected(notification);
                case APPLICATION_EXPIRED -> emailService.sendApplicationExpired(notification);
            }
        } else if (payload instanceof ApplicationPaymentNotification notification) {
            emailService.sendPaymentConfirmed(notification);
        } else {
            log.warn("Unknown payload type received on application-events: {}", payload.getClass());
        }
    }

    @KafkaListener(
            topics = "ticket-events",
            groupId = "notification-service-group",
            containerFactory = "ticketEventListenerFactory")
    public void consumeTicketEvent(ConsumerRecord<String, Object> record) {
        log.info("Received ticket event with key [{}]", record.key());

        if (record.value() instanceof TicketNotification notification) {
            emailService.sendTicket(notification);
        } else {
            log.warn("Unknown payload type received on ticket-events: {}", record.value().getClass());
        }
    }
}
