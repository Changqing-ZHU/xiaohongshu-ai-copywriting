package com.example.xhscopywriting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GenerationQueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenerationRepository generationRepository;

    @Test
    void returnsGenerationWhenIdExists() throws Exception {
        Generation generation = createProcessingGeneration();
        Long id = generationRepository.insert(generation);

        mockMvc.perform(get("/api/generations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.imageAnalysis").value((Object) null))
                .andExpect(jsonPath("$.title").value((Object) null))
                .andExpect(jsonPath("$.content").value((Object) null))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value((Object) null));
    }

    @Test
    void returnsNotFoundWhenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/generations/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Generation task not found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private Generation createProcessingGeneration() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation generation = new Generation();
        generation.setStatus("PROCESSING");
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);
        return generation;
    }
}
