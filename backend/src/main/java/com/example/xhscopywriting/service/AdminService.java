package com.example.xhscopywriting.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.dto.AdminDashboardResponse;
import com.example.xhscopywriting.dto.AdminGenerationResponse;
import com.example.xhscopywriting.dto.AdminUserResponse;
import com.example.xhscopywriting.repository.AdminRepository;

@Service
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                adminRepository.countUsers(),
                adminRepository.countGenerations(),
                adminRepository.countTodayGenerations(),
                adminRepository.countTodayActiveUsers());
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findUsers() {
        return adminRepository.findAllUsers()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminGenerationResponse> findGenerations() {
        return adminRepository.findAllGenerations()
                .stream()
                .map(AdminGenerationResponse::from)
                .toList();
    }
}
