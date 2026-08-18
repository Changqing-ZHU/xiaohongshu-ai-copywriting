package com.example.xhscopywriting.dto;

import java.time.LocalDateTime;

import com.example.xhscopywriting.model.AdminGenerationSummary;

public record AdminGenerationResponse(
        Long id,
        String username,
        String status,
        String imageUrl,
        String originalFileName,
        String imageContentType,
        Long imageSize,
        String title,
        LocalDateTime createdAt) {

    public static AdminGenerationResponse from(AdminGenerationSummary generation) {
        return new AdminGenerationResponse(
                generation.id(),
                generation.username(),
                generation.status(),
                generation.hasImage()
                        ? "/api/generations/" + generation.id() + "/image"
                        : null,
                generation.originalFileName(),
                generation.imageContentType(),
                generation.imageSize(),
                generation.title(),
                generation.createdAt());
    }
}
