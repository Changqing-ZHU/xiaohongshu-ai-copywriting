package com.example.xhscopywriting.model;

import java.time.LocalDateTime;

public record AdminGenerationSummary(
        Long id,
        String username,
        String status,
        String originalFileName,
        String imageContentType,
        Long imageSize,
        boolean hasImage,
        String title,
        LocalDateTime createdAt) {
}
