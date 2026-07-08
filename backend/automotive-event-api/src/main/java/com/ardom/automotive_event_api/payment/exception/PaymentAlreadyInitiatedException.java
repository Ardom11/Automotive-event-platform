package com.ardom.automotive_event_api.payment.exception;

public class PaymentAlreadyInitiatedException extends RuntimeException {
    public PaymentAlreadyInitiatedException(String message) {
        super(message);
    }
}
