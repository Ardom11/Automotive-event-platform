package com.ardom.automotive_event_api.event.exception;

public class DeletingNotDraftEventException extends RuntimeException {
    public DeletingNotDraftEventException(String message) {
        super(message);
    }
}
