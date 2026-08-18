package com.example.xhscopywriting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties =
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters")
class AdminAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        TestAuthentication.Identity admin = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider,
                "ADMIN");

        mockMvc.perform(get("/api/admin/access")
                        .header(HttpHeaders.AUTHORIZATION, admin.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(admin.user().getUsername()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.message").value("Admin access granted"));
    }

    @Test
    void userCannotAccessAdminEndpoint() throws Exception {
        TestAuthentication.Identity user = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);

        for (String path : adminPaths()) {
            mockMvc.perform(get(path)
                            .header(HttpHeaders.AUTHORIZATION, user.authorizationHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("Administrator access required"));
        }
    }

    @Test
    void unauthenticatedRequestCannotAccessAdminEndpoint() throws Exception {
        for (String path : adminPaths()) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }
    }

    @Test
    void ordinaryUserGenerationFlowRemainsAvailable() throws Exception {
        TestAuthentication.Identity user = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);

        mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, user.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        mockMvc.perform(get("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, user.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private String[] adminPaths() {
        return new String[] {
                "/api/admin/access",
                "/api/admin/dashboard",
                "/api/admin/users",
                "/api/admin/generations"
        };
    }
}
