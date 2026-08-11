package com.example.xhscopywriting.dto;

import java.util.List;

public record AiCopywritingResult(
        String imageAnalysis,
        String title,
        String content,
        List<String> tags) {
}
