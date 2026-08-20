package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.dto.AiCopywritingOptimizationInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;
import com.example.xhscopywriting.service.AiCopywritingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "security.jwt.secret=test-jwt-secret-with-at-least-32-characters")
class GenerationOptimizationControllerTests {

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
    private AiCopywritingService aiCopywritingService;

    @Test
    void createsNewGenerationWithOptimizedCopywritingAndKeepsOriginal() throws Exception {
        TestAuthentication.Identity identity = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        Generation original = completedGeneration(identity.user().getId());
        generationRepository.insert(original);
        when(aiCopywritingService.optimize(anyLong(), any()))
                .thenReturn(new AiCopywritingResult(
                        "原图片内容保持不变",
                        "更自然的新标题",
                        "像朋友一样分享的优化正文。",
                        List.of("自然分享", "生活记录", "轻松表达")));

        String responseBody = mockMvc.perform(post(
                        "/api/generations/{id}/optimize",
                        original.getId())
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"像朋友分享一样\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.title").value("更自然的新标题"))
                .andExpect(jsonPath("$.content").value("像朋友一样分享的优化正文。"))
                .andExpect(jsonPath("$.tags[0]").value("自然分享"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        Generation optimized = generationRepository
                .findById(response.get("id").asLong())
                .orElseThrow();
        Generation unchangedOriginal = generationRepository
                .findById(original.getId())
                .orElseThrow();

        assertNotEquals(original.getId(), optimized.getId());
        assertEquals(original.getUserId(), optimized.getUserId());
        assertEquals(original.getImagePath(), optimized.getImagePath());
        assertEquals("原始标题", unchangedOriginal.getTitle());
        assertEquals("原始正文", unchangedOriginal.getContent());

        ArgumentCaptor<AiCopywritingOptimizationInput> inputCaptor =
                ArgumentCaptor.forClass(AiCopywritingOptimizationInput.class);
        org.mockito.Mockito.verify(aiCopywritingService)
                .optimize(org.mockito.ArgumentMatchers.eq(optimized.getId()), inputCaptor.capture());
        assertEquals("像朋友分享一样", inputCaptor.getValue().instruction());
        assertEquals("原始正文", inputCaptor.getValue().originalContent());
        assertEquals(original.getImagePath(), inputCaptor.getValue().image().imagePath());
    }

    @Test
    void rejectsBlankOptimizationInstruction() throws Exception {
        TestAuthentication.Identity identity = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        Generation original = completedGeneration(identity.user().getId());
        generationRepository.insert(original);

        mockMvc.perform(post("/api/generations/{id}/optimize", original.getId())
                        .header(HttpHeaders.AUTHORIZATION, identity.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Please provide an optimization instruction."));
    }

    @Test
    void doesNotAllowAnotherUserToOptimizeGeneration() throws Exception {
        TestAuthentication.Identity owner = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        TestAuthentication.Identity anotherUser = TestAuthentication.createUser(
                userRepository,
                jwtTokenProvider);
        Generation original = completedGeneration(owner.user().getId());
        generationRepository.insert(original);

        mockMvc.perform(post("/api/generations/{id}/optimize", original.getId())
                        .header(HttpHeaders.AUTHORIZATION, anotherUser.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"简短一些\"}"))
                .andExpect(status().isNotFound());
    }

    private Generation completedGeneration(Long userId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation generation = new Generation();
        generation.setUserId(userId);
        generation.setStatus("COMPLETED");
        generation.setOriginalFileName("original.jpg");
        generation.setStoredFileName("stored.jpg");
        generation.setImagePath("uploads/stored.jpg");
        generation.setImageContentType(MediaType.IMAGE_JPEG_VALUE);
        generation.setImageSize(128L);
        generation.setImageAnalysis("原始图片分析");
        generation.setTitle("原始标题");
        generation.setContent("原始正文");
        generation.setTags("原始标签,图片分享");
        generation.setErrorMessage(null);
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);
        return generation;
    }
}
