package com.example.xhscopywriting.dto;

import java.time.LocalDateTime;

public record GenerationCreatedResponse(
        Long id,
        String status,
        LocalDateTime createdAt) {
}
