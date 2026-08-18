package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties =
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters")
class GenerationUserIsolationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void eachUserOnlySeesTheirOwnGenerationHistory() throws Exception {
        TestAuthentication.Identity userA = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        TestAuthentication.Identity userB = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);

        Long generationA = createGeneration(userA.authorizationHeader());
        Long generationB = createGeneration(userB.authorizationHeader());

        Generation savedA = generationRepository.findById(generationA).orElseThrow();
        Generation savedB = generationRepository.findById(generationB).orElseThrow();
        assertEquals(userA.user().getId(), savedA.getUserId());
        assertEquals(userB.user().getId(), savedB.getUserId());

        mockMvc.perform(get("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, userA.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(generationA));

        mockMvc.perform(get("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, userB.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(generationB));
    }

    @Test
    void creationAndHistoryRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        mockMvc.perform(get("/api/generations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    private Long createGeneration(String authorizationHeader) throws Exception {
        String response = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }
}
