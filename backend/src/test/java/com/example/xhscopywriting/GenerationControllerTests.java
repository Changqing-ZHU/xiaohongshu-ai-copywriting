package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "security.jwt.secret=test-jwt-secret-with-at-least-32-characters")
class GenerationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createsProcessingGenerationAndPersistsIt() throws Exception {
        TestAuthentication.Identity identity = authenticatedUser();
        String responseBody = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseBody);
        Long id = responseJson.get("id").asLong();
        Optional<Generation> savedGeneration = generationRepository.findById(id);

        assertTrue(savedGeneration.isPresent());
        Generation generation = savedGeneration.orElseThrow();
        assertEquals(identity.user().getId(), generation.getUserId());
        assertEquals("PROCESSING", generation.getStatus());
        assertNotNull(generation.getCreatedAt());
        assertEquals(generation.getCreatedAt(), generation.getUpdatedAt());
        assertNull(generation.getOriginalFileName());
        assertNull(generation.getImageAnalysis());
        assertNull(generation.getTitle());
        assertNull(generation.getContent());
        assertNull(generation.getTags());
        assertNull(generation.getErrorMessage());
    }

    @Test
    void persistsOptionalSourceUrlWhenCreatingGeneration() throws Exception {
        TestAuthentication.Identity identity = authenticatedUser();
        String responseBody = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/article\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();
        Generation generation = generationRepository.findById(id).orElseThrow();
        assertEquals("https://example.com/article", generation.getSourceUrl());
    }

    @Test
    void acceptsOptionalCopywritingStyleWhenCreatingGeneration() throws Exception {
        TestAuthentication.Identity identity = authenticatedUser();
        mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"style\":\"healing\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void persistsEnhancedGenerationOptions() throws Exception {
        TestAuthentication.Identity identity = authenticatedUser();
        String responseBody = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style":"viral",
                                  "scene":"food",
                                  "audience":"young_women",
                                  "ageGroup":"18_25",
                                  "marketingLevel":"strong",
                                  "length":"detailed",
                                  "emojiPreference":"rich"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();
        Generation generation = generationRepository.findById(id).orElseThrow();
        JsonNode options = objectMapper.readTree(generation.getGenerationOptions());

        assertEquals("viral", options.get("style").asText());
        assertEquals("food", options.get("scene").asText());
        assertEquals("young_women", options.get("audience").asText());
        assertEquals("18_25", options.get("ageGroup").asText());
        assertEquals("strong", options.get("marketingLevel").asText());
        assertEquals("detailed", options.get("length").asText());
        assertEquals("rich", options.get("emojiPreference").asText());
    }

    @Test
    void appliesBackwardCompatibleDefaultsWhenOptionsAreMissing() throws Exception {
        TestAuthentication.Identity identity = authenticatedUser();
        String responseBody = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();
        JsonNode options = objectMapper.readTree(
                generationRepository.findById(id).orElseThrow().getGenerationOptions());

        assertEquals("daily", options.get("style").asText());
        assertEquals("daily_record", options.get("scene").asText());
        assertEquals("general", options.get("audience").asText());
        assertEquals("unrestricted", options.get("ageGroup").asText());
        assertEquals("light", options.get("marketingLevel").asText());
        assertEquals("standard", options.get("length").asText());
        assertEquals("few", options.get("emojiPreference").asText());
    }

    private TestAuthentication.Identity authenticatedUser() {
        return TestAuthentication.createUser(userRepository, jwtTokenProvider);
    }
}
