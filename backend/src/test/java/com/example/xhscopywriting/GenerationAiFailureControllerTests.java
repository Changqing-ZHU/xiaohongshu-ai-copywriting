package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;
import com.example.xhscopywriting.service.AiCopywritingService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.upload-dir=target/test-ai-failure-uploads",
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters"
})
class GenerationAiFailureControllerTests {

    private static final String SAFE_ERROR_MESSAGE =
            "AI copywriting generation failed. Please try again later.";
    private static final Path TEST_UPLOAD_DIRECTORY = Path.of(
            "target",
            "test-ai-failure-uploads")
            .toAbsolutePath()
            .normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AiCopywritingService aiCopywritingService;

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
    void persistsFailedStatusAndKeepsImageWhenAiThrows() throws Exception {
        Generation generation = createGeneration();
        Long id = generationRepository.insert(generation);
        when(aiCopywritingService.generate(anyLong(), any(AiCopywritingInput.class)))
                .thenThrow(new IllegalStateException(
                        "Simulated third-party response containing sensitive details"));
        byte[] jpegContent = {
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF,
                (byte) 0xE0,
                0x00,
                0x10
        };
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "failed-generation.jpg",
                "image/jpeg",
                jpegContent);

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unable to generate copywriting"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        Generation failedGeneration = generationRepository.findById(id).orElseThrow();
        assertEquals("FAILED", failedGeneration.getStatus());
        assertEquals(SAFE_ERROR_MESSAGE, failedGeneration.getErrorMessage());
        assertNotNull(failedGeneration.getStoredFileName());
        assertNotNull(failedGeneration.getImagePath());
        assertTrue(Files.exists(Path.of(failedGeneration.getImagePath())));
        assertFalse(failedGeneration.getErrorMessage().contains("sensitive"));

        mockMvc.perform(get("/api/generations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(SAFE_ERROR_MESSAGE));
    }

    @Test
    void passesEnhancedOptionsFromCreateRequestToAiService() throws Exception {
        TestAuthentication.Identity identity = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        when(aiCopywritingService.generate(anyLong(), any(AiCopywritingInput.class)))
                .thenReturn(new AiCopywritingResult(
                        "图片内容分析",
                        "治愈风格标题",
                        "治愈风格正文",
                        List.of("治愈", "生活", "日常")));
        String createResponse = mockMvc.perform(post("/api/generations")
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style":"authentic",
                                  "contentType":"travel",
                                  "targetAudience":"couples",
                                  "ageGroup":"25_35",
                                  "recommendationLevel":"share",
                                  "copyLength":"short",
                                  "emojiPreference":"none"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse)
                .get("id")
                .asLong();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "healing.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10});

        mockMvc.perform(multipart("/api/generations/{id}/image", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        ArgumentCaptor<AiCopywritingInput> inputCaptor = ArgumentCaptor.forClass(
                AiCopywritingInput.class);
        verify(aiCopywritingService).generate(eq(id), inputCaptor.capture());
        assertEquals("authentic", inputCaptor.getValue().options().style());
        assertEquals("travel", inputCaptor.getValue().options().scene());
        assertEquals("couples", inputCaptor.getValue().options().audience());
        assertEquals("25_35", inputCaptor.getValue().options().ageGroup());
        assertEquals("share", inputCaptor.getValue().options().marketingLevel());
        assertEquals("short", inputCaptor.getValue().options().length());
        assertEquals("none", inputCaptor.getValue().options().emojiPreference());
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
