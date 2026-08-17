package com.example.xhscopywriting.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record GenerationImageResource(
        Resource resource,
        MediaType contentType,
        long contentLength) {
}
