package com.example.xhscopywriting.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.xhscopywriting.dto.AdminAccessResponse;
import com.example.xhscopywriting.dto.AdminDashboardResponse;
import com.example.xhscopywriting.dto.AdminGenerationResponse;
import com.example.xhscopywriting.dto.AdminUserResponse;
import com.example.xhscopywriting.model.User;
import com.example.xhscopywriting.security.CurrentUserService;
import com.example.xhscopywriting.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CurrentUserService currentUserService;
    private final AdminService adminService;

    public AdminController(
            CurrentUserService currentUserService,
            AdminService adminService) {
        this.currentUserService = currentUserService;
        this.adminService = adminService;
    }

    @GetMapping("/access")
    public ResponseEntity<AdminAccessResponse> verifyAccess(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        User admin = currentUserService.requireAdmin(authorizationHeader);
        return ResponseEntity.ok(new AdminAccessResponse(
                admin.getUsername(),
                admin.getRole(),
                "Admin access granted"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        currentUserService.requireAdmin(authorizationHeader);
        return ResponseEntity.ok(adminService.dashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> findUsers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        currentUserService.requireAdmin(authorizationHeader);
        return ResponseEntity.ok(adminService.findUsers());
    }

    @GetMapping("/generations")
    public ResponseEntity<List<AdminGenerationResponse>> findGenerations(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        currentUserService.requireAdmin(authorizationHeader);
        return ResponseEntity.ok(adminService.findGenerations());
    }
}
