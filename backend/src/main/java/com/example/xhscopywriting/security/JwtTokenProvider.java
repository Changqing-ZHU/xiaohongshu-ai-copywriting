package com.example.xhscopywriting.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.xhscopywriting.exception.JwtConfigurationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationMillis;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.expiration-ms:3600000}") long expirationMillis) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String username, String role) {
        validateConfiguration();
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationMillis / 1000;

        String header = encodeJson(Map.of("alg", JWT_ALGORITHM, "typ", "JWT"));
        String payload = encodeJson(Map.of(
                "sub", username,
                "role", role,
                "iat", issuedAt,
                "exp", expiresAt));
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public JwtClaims parseClaims(String token) {
        validateConfiguration();
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT is empty");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT format is invalid");
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("JWT signature is invalid");
        }

        JsonNode header = decodeJson(parts[0]);
        if (!JWT_ALGORITHM.equals(header.path("alg").asText())) {
            throw new IllegalArgumentException("JWT algorithm is invalid");
        }

        JsonNode payload = decodeJson(parts[1]);
        String username = payload.path("sub").asText();
        String role = payload.path("role").asText();
        long expiresAt = payload.path("exp").asLong(0);
        if (username.isBlank() || role.isBlank() || expiresAt <= Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("JWT claims are invalid or expired");
        }
        return new JwtClaims(username, role, Instant.ofEpochSecond(expiresAt));
    }

    private void validateConfiguration() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new JwtConfigurationException(
                    "JWT secret must contain at least 32 characters");
        }
        if (expirationMillis < 1000) {
            throw new JwtConfigurationException("JWT expiration must be positive");
        }
    }

    private String encodeJson(Object value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode JWT claims", exception);
        }
    }

    private JsonNode decodeJson(String value) {
        try {
            return objectMapper.readTree(BASE64_URL_DECODER.decode(value));
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT content is invalid", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(
                    mac.doFinal(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    public record JwtClaims(String username, String role, Instant expiresAt) {
    }
}
