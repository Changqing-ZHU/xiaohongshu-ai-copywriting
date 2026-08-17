package com.example.xhscopywriting.dto;

public record DownloadedImage(
        String originalFileName,
        String contentType,
        byte[] content) {
}
