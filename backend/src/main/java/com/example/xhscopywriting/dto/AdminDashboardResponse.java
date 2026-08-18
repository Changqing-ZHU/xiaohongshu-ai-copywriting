package com.example.xhscopywriting.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long totalGenerations,
        long todayGenerations,
        long todayActiveUsers) {
}
