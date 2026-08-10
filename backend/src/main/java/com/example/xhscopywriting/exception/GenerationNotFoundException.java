package com.example.xhscopywriting.exception;

public class GenerationNotFoundException extends RuntimeException {

    public GenerationNotFoundException(Long id) {
        super("Generation task not found: " + id);
    }
}
