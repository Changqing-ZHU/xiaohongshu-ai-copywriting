package com.example.xhscopywriting.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super("Username already exists");
    }
}
