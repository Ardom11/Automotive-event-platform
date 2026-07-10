package com.ardom.automotive_event_api.payment.exception;

public class WebhookException extends RuntimeException {
    public WebhookException(String message) {
        super(message);
    }
}
