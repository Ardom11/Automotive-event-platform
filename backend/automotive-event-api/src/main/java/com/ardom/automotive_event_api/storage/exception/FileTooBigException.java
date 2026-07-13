package com.ardom.automotive_event_api.storage.exception;

public class FileTooBigException extends RuntimeException {
    public FileTooBigException(String message) {
        super(message);
    }
}
