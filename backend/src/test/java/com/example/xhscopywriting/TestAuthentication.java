package com.example.xhscopywriting;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.xhscopywriting.model.User;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;

final class TestAuthentication {

    private TestAuthentication() {
    }

    static Identity createUser(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider) {
        User user = new User();
        user.setUsername("gen-" + UUID.randomUUID());
        user.setPassword("test-password-not-used-for-direct-test-fixture");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now().withNano(0));
        userRepository.insert(user);
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return new Identity(user, "Bearer " + token);
    }

    record Identity(User user, String authorizationHeader) {
    }
}
