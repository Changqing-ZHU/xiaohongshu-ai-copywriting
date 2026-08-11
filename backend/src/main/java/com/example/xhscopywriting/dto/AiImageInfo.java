package com.example.xhscopywriting.dto;

public record AiImageInfo(
        String originalFileName,
        String storedFileName,
        String imagePath,
        String contentType,
        long size) {
}
