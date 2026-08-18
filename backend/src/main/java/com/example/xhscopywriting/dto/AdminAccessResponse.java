package com.example.xhscopywriting.dto;

public record AdminAccessResponse(
        String username,
        String role,
        String message) {
}
