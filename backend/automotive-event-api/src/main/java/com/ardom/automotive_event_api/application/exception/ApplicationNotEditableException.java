package com.ardom.automotive_event_api.application.exception;

public class ApplicationNotEditableException extends RuntimeException {
    public ApplicationNotEditableException(String message) {
        super(message);
    }
}
