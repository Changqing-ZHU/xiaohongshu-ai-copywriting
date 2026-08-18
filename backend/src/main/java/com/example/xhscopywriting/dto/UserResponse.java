package com.example.xhscopywriting.dto;

import java.time.LocalDateTime;

import com.example.xhscopywriting.model.User;

public record UserResponse(
        Long id,
        String username,
        String role,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt());
    }
}
