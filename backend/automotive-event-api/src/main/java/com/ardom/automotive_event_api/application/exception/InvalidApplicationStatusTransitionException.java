package com.ardom.automotive_event_api.application.exception;

public class InvalidApplicationStatusTransitionException extends RuntimeException {
    public InvalidApplicationStatusTransitionException(String message) {
        super(message);
    }
}
