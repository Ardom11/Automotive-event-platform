package com.ardom.automotive_event_api.event.exception;

public class EventSoldOutException extends RuntimeException {
    public EventSoldOutException(String message) {
        super(message);
    }
}
