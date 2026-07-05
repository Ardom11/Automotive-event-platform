package com.ardom.automotive_event_api.application.exception;

public class ApplicationDeadlineException extends RuntimeException {
    public ApplicationDeadlineException(String message) {
        super(message);
    }
}
