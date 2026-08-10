package com.example.xhscopywriting.dto;

public record ImageStorageResult(
        String storedFileName,
        String imagePath,
        String contentType,
        long size) {
}
