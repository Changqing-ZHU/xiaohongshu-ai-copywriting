package com.example.xhscopywriting.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.example.xhscopywriting.model.Generation;

public record GenerationHistoryResponse(
        Long id,
        String status,
        String imageUrl,
        String title,
        String content,
        List<String> tags,
        LocalDateTime createdAt) {

    public static GenerationHistoryResponse from(Generation generation) {
        String imageUrl = generation.getImagePath() == null
                || generation.getImagePath().isBlank()
                ? null
                : "/api/generations/" + generation.getId() + "/image";
        return new GenerationHistoryResponse(
                generation.getId(),
                generation.getStatus(),
                imageUrl,
                generation.getTitle(),
                generation.getContent(),
                parseTags(generation.getTags()),
                generation.getCreatedAt());
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
