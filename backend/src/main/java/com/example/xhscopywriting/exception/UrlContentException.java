package com.example.xhscopywriting.exception;

public class UrlContentException extends RuntimeException {

    public UrlContentException(String message) {
        super(message);
    }

    public UrlContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
