package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
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
@TestPropertySource(properties = {
        "app.upload-dir=target/test-uploads",
        "ai.provider=mock",
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters"
})
class GenerationImageUploadControllerTests {

    private static final Path TEST_UPLOAD_DIRECTORY = Path.of("target", "test-uploads")
            .toAbsolutePath()
            .normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MultipartProperties multipartProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
                "holiday.jpeg",
                "image/jpg",
                jpegContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Generation saved = generationRepository.findById(id).orElseThrow();
        assertEquals("holiday.jpeg", saved.getOriginalFileName());
        assertNotNull(saved.getStoredFileName());
        assertNotEquals("holiday.jpeg", saved.getStoredFileName());
        assertTrue(saved.getStoredFileName().matches("[0-9a-f-]{36}\\.jpg"));
        assertEquals("image/jpeg", saved.getImageContentType());
        assertEquals((long) jpegContent.length, saved.getImageSize());
        assertEquals("COMPLETED", saved.getStatus());
        assertEquals(
                "模拟图片分析：画面氛围自然，主体清晰，适合分享生活方式内容。",
                saved.getImageAnalysis());
        assertEquals("把普通日子过成喜欢的样子 ✨", saved.getTitle());
        assertEquals(
                "这是一段由模拟 AI 服务生成的小红书正文，用于验证后端业务流程。",
                saved.getContent());
        assertEquals("生活记录,治愈日常,氛围感", saved.getTags());
        assertTrue(Path.of(saved.getImagePath()).startsWith(TEST_UPLOAD_DIRECTORY));
        assertTrue(Files.exists(Path.of(saved.getImagePath())));
    }

    @Test
    void acceptsImageSmallerThanTenMegabytes() throws Exception {
        assertEquals(10L * 1024 * 1024, multipartProperties.getMaxFileSize().toBytes());
        assertEquals(10L * 1024 * 1024, multipartProperties.getMaxRequestSize().toBytes());

        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        byte[] jpegContent = new byte[9 * 1024 * 1024];
        jpegContent[0] = (byte) 0xFF;
        jpegContent[1] = (byte) 0xD8;
        jpegContent[2] = (byte) 0xFF;
        jpegContent[3] = (byte) 0xE1;
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "phone-photo.jpeg",
                "application/octet-stream",
                jpegContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Generation saved = generationRepository.findById(id).orElseThrow();
        assertEquals((long) jpegContent.length, saved.getImageSize());
        assertEquals("image/jpeg", saved.getImageContentType());
        assertTrue(Files.exists(Path.of(saved.getImagePath())));
    }

    @Test
    void detectsPngFromContentWhenClientMetadataIsInaccurate() throws Exception {
        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        byte[] pngContent = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "wechat-download.jpg",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                pngContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Generation saved = generationRepository.findById(id).orElseThrow();
        assertEquals("wechat-download.jpg", saved.getOriginalFileName());
        assertEquals("image/png", saved.getImageContentType());
        assertTrue(saved.getStoredFileName().matches("[0-9a-f-]{36}\\.png"));
        assertTrue(Files.exists(Path.of(saved.getImagePath())));
    }

    @Test
    void acceptsWebpBasedOnItsContentSignature() throws Exception {
        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        byte[] webpContent = {
                'R', 'I', 'F', 'F', 0x04, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P'
        };
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "xiaohongshu-download",
                null,
                webpContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk());

        Generation saved = generationRepository.findById(id).orElseThrow();
        assertEquals("image/webp", saved.getImageContentType());
        assertTrue(saved.getStoredFileName().matches("[0-9a-f-]{36}\\.webp"));
    }

    @Test
    void createsTaskUploadsImageAndReturnsMockResultFromQueryApi() throws Exception {
        TestAuthentication.Identity identity = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        String creationResponse = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode responseJson = objectMapper.readTree(creationResponse);
        Long id = responseJson.get("id").asLong();
        byte[] jpegContent = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "day.jpg",
                "image/jpeg",
                jpegContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/generations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.imageAnalysis").value(
                        "模拟图片分析：画面氛围自然，主体清晰，适合分享生活方式内容。"))
                .andExpect(jsonPath("$.title").value("把普通日子过成喜欢的样子 ✨"))
                .andExpect(jsonPath("$.content").value(
                        "这是一段由模拟 AI 服务生成的小红书正文，用于验证后端业务流程。"))
                .andExpect(jsonPath("$.tags[0]").value("生活记录"))
                .andExpect(jsonPath("$.tags[1]").value("治愈日常"))
                .andExpect(jsonPath("$.tags[2]").value("氛围感"))
                .andExpect(jsonPath("$.errorMessage").value((Object) null));
    }

    @Test
    void rejectsNonImageContentEvenWhenMetadataClaimsJpeg() throws Exception {
        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        MockMultipartFile textFile = new MockMultipartFile(
                "image",
                "looks-safe.jpg",
                "image/jpeg",
                "not an image".getBytes());

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported or invalid image content"));

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
