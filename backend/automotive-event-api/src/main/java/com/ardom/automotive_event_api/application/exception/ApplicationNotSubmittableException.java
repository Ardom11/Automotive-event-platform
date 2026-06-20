package com.ardom.automotive_event_api.application.exception;

public class ApplicationNotSubmittableException extends RuntimeException {
    public ApplicationNotSubmittableException(String message) {
        super(message);
    }
}
