package com.example.xhscopywriting.exception;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String IMAGE_SIZE_LIMIT_MESSAGE =
            "Image size exceeds limit. Please upload an image smaller than 10MB.";

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationRequiredException(
            AuthenticationRequiredException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AdminAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminAccessDeniedException(
            AdminAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InvalidAuthInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAuthInputException(
            InvalidAuthInputException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(JwtConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtConfigurationException(
            JwtConfigurationException exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication service is not configured");
    }

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

    @ExceptionHandler(GenerationImageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationImageNotFoundException(
            GenerationImageNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Generation image not found",
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
        Throwable cause = exception.getCause();
        LOGGER.warn(
                "Returning AI generation failure response: exceptionType={}, causeType={}",
                exception.getClass().getSimpleName(),
                cause == null ? "none" : cause.getClass().getSimpleName());
        ApiErrorResponse response = new ApiErrorResponse(
                "Unable to generate copywriting",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                message,
                LocalDateTime.now()));
    }
}
