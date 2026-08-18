package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.User;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters",
        "security.jwt.expiration-ms=3600000"
})
class AuthControllerTests {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void registersUserAndStoresBcryptPassword() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByUsername(username).orElseThrow();
        assertNotEquals(PASSWORD, saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void logsInAndReturnsValidJwt() throws Exception {
        String username = uniqueUsername();
        register(username);

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        String token = response.get("token").asText();
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.parseClaims(token).username().equals(username));
    }

    @Test
    void rejectsIncorrectPassword() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void logsOutStatelessSession() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String credentials(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new Credentials(username, password));
    }

    private String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record Credentials(String username, String password) {
    }
}
