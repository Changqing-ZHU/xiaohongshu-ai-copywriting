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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GenerationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GenerationRepository generationRepository;

    @Test
    void createsProcessingGenerationAndPersistsIt() throws Exception {
        String responseBody = mockMvc.perform(post("/api/generations")
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
}
