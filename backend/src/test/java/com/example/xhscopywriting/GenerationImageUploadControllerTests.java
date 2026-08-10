package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.upload-dir=target/test-uploads")
class GenerationImageUploadControllerTests {

    private static final Path TEST_UPLOAD_DIRECTORY = Path.of("target", "test-uploads")
            .toAbsolutePath()
            .normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenerationRepository generationRepository;

    @AfterEach
    void deleteUploadedTestFiles() throws IOException {
        if (!Files.exists(TEST_UPLOAD_DIRECTORY)) {
            return;
        }

        try (Stream<Path> files = Files.list(TEST_UPLOAD_DIRECTORY)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
        Files.deleteIfExists(TEST_UPLOAD_DIRECTORY);
    }

    @Test
    void uploadsJpegAndUpdatesGenerationMetadata() throws Exception {
        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        byte[] jpegContent = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "holiday.jpg",
                "image/jpeg",
                jpegContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        Generation saved = generationRepository.findById(id).orElseThrow();
        assertEquals("holiday.jpg", saved.getOriginalFileName());
        assertNotNull(saved.getStoredFileName());
        assertNotEquals("holiday.jpg", saved.getStoredFileName());
        assertTrue(saved.getStoredFileName().matches("[0-9a-f-]{36}\\.jpg"));
        assertEquals("image/jpeg", saved.getImageContentType());
        assertEquals((long) jpegContent.length, saved.getImageSize());
        assertEquals("PROCESSING", saved.getStatus());
        assertTrue(Path.of(saved.getImagePath()).startsWith(TEST_UPLOAD_DIRECTORY));
        assertTrue(Files.exists(Path.of(saved.getImagePath())));
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        MockMultipartFile textFile = new MockMultipartFile(
                "image",
                "notes.txt",
                "text/plain",
                "not an image".getBytes());

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported image type"));

        Generation saved = generationRepository.findById(id).orElseThrow();
        assertEquals("PROCESSING", saved.getStatus());
        assertNull(saved.getStoredFileName());
        assertFalse(Files.exists(TEST_UPLOAD_DIRECTORY));
    }

    @Test
    void returnsNotFoundBeforeStoringImage() throws Exception {
        byte[] jpegContent = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "holiday.jpg",
                "image/jpeg",
                jpegContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", Long.MAX_VALUE).file(image))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Generation task not found"));

        assertFalse(Files.exists(TEST_UPLOAD_DIRECTORY));
    }

    private Generation createGeneration() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation generation = new Generation();
        generation.setStatus("PROCESSING");
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);
        return generation;
    }
}
