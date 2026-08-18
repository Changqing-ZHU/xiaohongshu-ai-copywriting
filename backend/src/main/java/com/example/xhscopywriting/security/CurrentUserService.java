package com.example.xhscopywriting.security;

import org.springframework.stereotype.Service;

import com.example.xhscopywriting.exception.AuthenticationRequiredException;
import com.example.xhscopywriting.model.User;
import com.example.xhscopywriting.repository.UserRepository;

@Service
public class CurrentUserService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public CurrentUserService(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    public User requireUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationRequiredException();
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtTokenProvider.JwtClaims claims = jwtTokenProvider.parseClaims(token);
            return userRepository.findByUsername(claims.username())
                    .orElseThrow(AuthenticationRequiredException::new);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationRequiredException();
        }
    }
}
