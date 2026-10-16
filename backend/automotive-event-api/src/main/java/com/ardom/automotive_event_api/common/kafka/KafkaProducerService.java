package com.ardom.automotive_event_api.common.kafka;

import com.ardom.automotive_event_api.common.notification.ApplicationNotification;
import com.ardom.automotive_event_api.common.notification.ApplicationPaymentNotification;
import com.ardom.automotive_event_api.common.notification.TicketNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendApplicationNotification(ApplicationNotification notification) {
        send("application-events", notification.applicationId().toString(), notification);
    }

    public void sendApplicationPaymentNotification(ApplicationPaymentNotification notification) {
        send("application-events", notification.applicationId().toString(), notification);
    }

    public void sendTicketNotification(TicketNotification notification) {
        send("ticket-events", notification.recipientEmail(), notification);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event to topic [{}] with key [{}]", topic, key, ex);
                    } else {
                        log.info("Event sent to topic [{}] partition [{}] offset [{}]",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
