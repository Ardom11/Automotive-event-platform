package com.ardom.automotive_event_api.application.exception;

public class TooManyCarsException extends RuntimeException {
    public TooManyCarsException(String message) {
        super(message);
    }
}
