package com.example.xhscopywriting.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String IMAGE_SIZE_LIMIT_MESSAGE =
            "Image size exceeds limit. Please upload an image smaller than 10MB.";

    @ExceptionHandler(InvalidGenerationInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGenerationInputException(
            InvalidGenerationInputException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getMessage(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UrlContentException.class)
    public ResponseEntity<ApiErrorResponse> handleUrlContentException(
            UrlContentException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getMessage(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                IMAGE_SIZE_LIMIT_MESSAGE,
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

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

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidImageException(
            InvalidImageException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                exception.getMessage(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({ImageStorageException.class, ImageUploadException.class})
    public ResponseEntity<ApiErrorResponse> handleImageUploadFailure(RuntimeException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Unable to store generation image",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(GenerationProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationProcessingException(
            GenerationProcessingException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Unable to generate copywriting",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
