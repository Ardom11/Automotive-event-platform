package com.ardom.automotive_event_api.application.exception;

public class TicketPaymentNotFoundException extends RuntimeException {
    public TicketPaymentNotFoundException(String message) {
        super(message);
    }
}
