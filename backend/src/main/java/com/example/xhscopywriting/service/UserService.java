package com.example.xhscopywriting.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.dto.LoginRequest;
import com.example.xhscopywriting.dto.LoginResponse;
import com.example.xhscopywriting.dto.RegisterRequest;
import com.example.xhscopywriting.exception.InvalidAuthInputException;
import com.example.xhscopywriting.exception.InvalidCredentialsException;
import com.example.xhscopywriting.exception.UserAlreadyExistsException;
import com.example.xhscopywriting.model.User;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;

@Service
public class UserService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String username = validateUsername(request == null ? null : request.username());
        String password = validatePassword(request == null ? null : request.password());
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException();
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(DEFAULT_ROLE);
        user.setCreatedAt(LocalDateTime.now().withNano(0));
        try {
            userRepository.insert(user);
            return user;
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = normalizeLoginUsername(request == null ? null : request.username());
        String password = request == null ? null : request.password();
        if (username == null || password == null) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getRole());
    }

    private String validateUsername(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAuthInputException("Username is required");
        }
        String username = value.trim();
        if (username.length() < 3 || username.length() > 50) {
            throw new InvalidAuthInputException(
                    "Username must contain between 3 and 50 characters");
        }
        return username;
    }

    private String validatePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAuthInputException("Password is required");
        }
        int byteLength = value.getBytes(StandardCharsets.UTF_8).length;
        if (value.length() < 6 || byteLength > 72) {
            throw new InvalidAuthInputException(
                    "Password must contain at least 6 characters and at most 72 bytes");
        }
        return value;
    }

    private String normalizeLoginUsername(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
