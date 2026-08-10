package com.example.xhscopywriting.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String message,
        LocalDateTime timestamp) {
}
