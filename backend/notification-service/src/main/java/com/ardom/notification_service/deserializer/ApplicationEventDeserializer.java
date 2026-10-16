package com.ardom.notification_service.deserializer;

import com.ardom.notification_service.model.ApplicationNotification;
import com.ardom.notification_service.model.ApplicationPaymentNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class ApplicationEventDeserializer implements Deserializer<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public Object deserialize(String topic, byte[] data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            if (node.has("eventType")) {
                return objectMapper.treeToValue(node, ApplicationNotification.class);
            } else {
                return objectMapper.treeToValue(node, ApplicationPaymentNotification.class);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize application event", e);
            throw new RuntimeException(e);
        }
    }
}
