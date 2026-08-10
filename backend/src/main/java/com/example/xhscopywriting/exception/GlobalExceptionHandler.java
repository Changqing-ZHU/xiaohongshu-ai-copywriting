package com.example.xhscopywriting.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GenerationCreationException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationCreationException(
            GenerationCreationException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Unable to create generation task",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(GenerationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationNotFoundException(
            GenerationNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Generation task not found",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
