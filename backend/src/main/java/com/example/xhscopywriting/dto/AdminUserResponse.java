package com.example.xhscopywriting.dto;

import java.time.LocalDateTime;

import com.example.xhscopywriting.model.User;

public record AdminUserResponse(
        String username,
        String role,
        LocalDateTime createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt());
    }
}
