package com.example.xhscopywriting.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.example.xhscopywriting.model.Generation;

public record GenerationResponse(
        Long id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String imageAnalysis,
        String title,
        String content,
        List<String> tags,
        String errorMessage) {

    public static GenerationResponse from(Generation generation) {
        return new GenerationResponse(
                generation.getId(),
                generation.getStatus(),
                generation.getCreatedAt(),
                generation.getUpdatedAt(),
                generation.getImageAnalysis(),
                generation.getTitle(),
                generation.getContent(),
                parseTags(generation.getTags()),
                generation.getErrorMessage());
    }

    private static List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}
