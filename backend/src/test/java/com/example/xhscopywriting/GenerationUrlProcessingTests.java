package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.xhscopywriting.dto.DownloadedImage;
import com.example.xhscopywriting.exception.UrlContentException;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;
import com.example.xhscopywriting.service.UrlContentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=target/test-url-uploads",
        "ai.provider=mock",
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters"
})
class GenerationUrlProcessingTests {

    private static final String SOURCE_URL = "https://example.com/photo.jpg";
    private static final String SAFE_URL_ACCESS_ERROR =
            "Unable to access image URL. Please check the URL and try again.";
    private static final String SAFE_URL_FORMAT_ERROR =
            "Unsupported image URL format. Please use JPEG, PNG, or WebP.";
    private static final String SAFE_URL_SIZE_ERROR =
            "Image size exceeds limit. Please use an image smaller than 10MB.";
    private static final Path TEST_UPLOAD_DIRECTORY = Path.of("target", "test-url-uploads")
            .toAbsolutePath()
            .normalize();

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

    @MockBean
    private UrlContentService urlContentService;

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
    void generatesAsynchronouslyFromImageUrlAndStoresDownloadedImage() throws Exception {
        when(urlContentService.downloadImage(SOURCE_URL)).thenReturn(new DownloadedImage(
                "cover.jpg",
                "image/jpeg",
                jpegBytes()));
        Long id = createGeneration(SOURCE_URL);

        mockMvc.perform(post("/api/generations/{id}/generate", id))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        Generation completed = awaitTerminalStatus(id);
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getImagePath());
        assertTrue(Files.exists(Path.of(completed.getImagePath())));
        assertNotNull(completed.getTitle());
    }

    @Test
    void marksImageUrlGenerationFailedWhenUrlCannotBeAccessed() throws Exception {
        assertFailureMessage(SAFE_URL_ACCESS_ERROR);
    }

    @Test
    void reportsUnsupportedImageUrlFormat() throws Exception {
        assertFailureMessage(SAFE_URL_FORMAT_ERROR);
    }

    @Test
    void reportsOversizedImageUrl() throws Exception {
        assertFailureMessage(SAFE_URL_SIZE_ERROR);
    }

    @Test
    void rejectsGenerationTriggerWhenNeitherImageNorUrlExists() throws Exception {
        Long id = createGeneration(null);

        mockMvc.perform(post("/api/generations/{id}/generate", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please provide an image or URL."));
    }

    @Test
    void combinesUploadedImageWithImageUrl() throws Exception {
        when(urlContentService.downloadImage(SOURCE_URL)).thenReturn(new DownloadedImage(
                "url-image.webp",
                "image/webp",
                webpBytes()));
        Long id = createGeneration(SOURCE_URL);

        mockMvc.perform(multipart("/api/generations/{id}/image", id)
                        .file(uploadedImage()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Generation completed = generationRepository.findById(id).orElseThrow();
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getImagePath());
    }

    @Test
    void imageUrlFailureDoesNotPreventUploadedImageGeneration() throws Exception {
        when(urlContentService.downloadImage(SOURCE_URL))
                .thenThrow(new UrlContentException(SAFE_URL_ACCESS_ERROR));
        Long id = createGeneration(SOURCE_URL);

        mockMvc.perform(multipart("/api/generations/{id}/image", id)
                        .file(uploadedImage()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Generation completed = generationRepository.findById(id).orElseThrow();
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getTitle());
    }

    private void assertFailureMessage(String message) throws Exception {
        when(urlContentService.downloadImage(SOURCE_URL))
                .thenThrow(new UrlContentException(message));
        Long id = createGeneration(SOURCE_URL);

        mockMvc.perform(post("/api/generations/{id}/generate", id))
                .andExpect(status().isAccepted());

        Generation failed = awaitTerminalStatus(id);
        assertEquals("FAILED", failed.getStatus());
        assertEquals(message, failed.getErrorMessage());
    }

    private Long createGeneration(String url) throws Exception {
        TestAuthentication.Identity identity = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        String body = url == null ? "{}" : "{\"url\":\"" + url + "\"}";
        String response = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private Generation awaitTerminalStatus(Long id) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            Generation generation = generationRepository.findById(id).orElseThrow();
            if (!"PROCESSING".equals(generation.getStatus())) {
                return generation;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Generation did not reach a terminal status in time");
    }

    private MockMultipartFile uploadedImage() {
        return new MockMultipartFile(
                "image",
                "user.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                jpegBytes());
    }

    private byte[] jpegBytes() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10
        };
    }

    private byte[] webpBytes() {
        return new byte[] {
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
        };
    }
}
