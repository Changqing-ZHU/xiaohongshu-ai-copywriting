package com.example.xhscopywriting.exception;

public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException() {
        super("Administrator access required");
    }
}
