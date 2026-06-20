package com.ardom.automotive_event_api.application.exception;

public class UserAlreadyHasApplicationException extends RuntimeException {
    public UserAlreadyHasApplicationException(String message) {
        super(message);
    }
}
