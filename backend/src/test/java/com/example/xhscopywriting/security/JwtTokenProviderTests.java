package com.example.xhscopywriting.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class JwtTokenProviderTests {

    private static final String SECRET = "unit-test-jwt-secret-with-at-least-32-characters";

    @Test
    void generatesAndValidatesSignedTokenWithIdentityClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(
                new ObjectMapper(),
                SECRET,
                3_600_000);

        String token = provider.generateToken("test", "USER");
        JwtTokenProvider.JwtClaims claims = provider.parseClaims(token);

        assertTrue(provider.validateToken(token));
        assertEquals("test", claims.username());
        assertEquals("USER", claims.role());
        assertTrue(claims.expiresAt().isAfter(java.time.Instant.now()));
    }

    @Test
    void rejectsTokenWithModifiedSignature() {
        JwtTokenProvider provider = new JwtTokenProvider(
                new ObjectMapper(),
                SECRET,
                3_600_000);
        String token = provider.generateToken("test", "USER");
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertFalse(provider.validateToken(tampered));
    }
}
