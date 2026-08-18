package com.example.xhscopywriting.dto;

public record LoginResponse(
        String token,
        String username,
        String role) {
}
