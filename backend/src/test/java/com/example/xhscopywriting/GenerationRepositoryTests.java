package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;

@SpringBootTest
@Transactional
class GenerationRepositoryTests {

    @Autowired
    private GenerationRepository generationRepository;

    @Test
    void insertsAndFindsGenerationById() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation generation = createTestGeneration(now);

        Long id = generationRepository.insert(generation);
        Optional<Generation> savedGeneration = generationRepository.findById(id);

        assertNotNull(id);
        assertEquals(id, generation.getId());
        assertTrue(savedGeneration.isPresent());

        Generation result = savedGeneration.orElseThrow();
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("https://example.com/article", result.getSourceUrl());
        assertEquals("Example article", result.getUrlTitle());
        assertEquals("Example description", result.getUrlDescription());
        assertEquals("test-image.jpg", result.getOriginalFileName());
        assertEquals("stored-test-image.jpg", result.getStoredFileName());
        assertEquals("uploads/test/stored-test-image.jpg", result.getImagePath());
        assertEquals("image/jpeg", result.getImageContentType());
        assertEquals(1024L, result.getImageSize());
        assertEquals("A test image analysis", result.getImageAnalysis());
        assertEquals("Test title", result.getTitle());
        assertEquals("Test content", result.getContent());
        assertEquals("test,example", result.getTags());
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
    }

    private Generation createTestGeneration(LocalDateTime now) {
        Generation generation = new Generation();
        generation.setStatus("COMPLETED");
        generation.setSourceUrl("https://example.com/article");
        generation.setUrlTitle("Example article");
        generation.setUrlDescription("Example description");
        generation.setOriginalFileName("test-image.jpg");
        generation.setStoredFileName("stored-test-image.jpg");
        generation.setImagePath("uploads/test/stored-test-image.jpg");
        generation.setImageContentType("image/jpeg");
        generation.setImageSize(1024L);
        generation.setImageAnalysis("A test image analysis");
        generation.setTitle("Test title");
        generation.setContent("Test content");
        generation.setTags("test,example");
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);
        return generation;
    }
}
