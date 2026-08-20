package com.example.xhscopywriting.dto;

public record AiCopywritingOptimizationInput(
        AiImageInfo image,
        String urlTitle,
        String urlDescription,
        String originalImageAnalysis,
        String originalTitle,
        String originalContent,
        String originalTags,
        String instruction) {
}
