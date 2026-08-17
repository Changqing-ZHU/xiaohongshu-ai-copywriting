package com.example.xhscopywriting.exception;

public class GenerationImageNotFoundException extends RuntimeException {

    public GenerationImageNotFoundException(Long generationId) {
        super("Generation image not found: " + generationId);
    }
}
