package com.ardom.automotive_event_api.application.exception;

public class ApplicationPaymentNotFoundException extends RuntimeException {
    public ApplicationPaymentNotFoundException(String message) {
        super(message);
    }
}
