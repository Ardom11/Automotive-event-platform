package com.ardom.automotive_event_api.event.exception;

public class NotEnoughEventTicketsException extends RuntimeException {
    public NotEnoughEventTicketsException(String message) {
        super(message);
    }
}
