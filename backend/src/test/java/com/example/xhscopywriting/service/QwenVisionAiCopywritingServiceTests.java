package com.example.xhscopywriting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

class QwenVisionAiCopywritingServiceTests {

    private static final String BASE_URL = "https://dashscope.test/compatible-mode/v1";
    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "account-qwen-vision-model";

    @TempDir
    Path tempDirectory;

    private ObjectMapper objectMapper;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private Path imagePath;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        imagePath = tempDirectory.resolve("stored-image.jpg");
        Files.write(imagePath, new byte[] {
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF,
                (byte) 0xE0,
                0x00,
                0x10
        });
    }

    @Test
    void sendsQwenImageRequestAndParsesStructuredJson() throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "画面中是一杯放在木桌上的咖啡。",
                "title", "一杯咖啡的治愈时刻",
                "content", "慢下来，享受眼前这一杯咖啡。",
                "tags", List.of("咖啡日常", "生活记录", "治愈时刻")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[1].content[0].type").value("image_url"))
                .andExpect(jsonPath("$.messages[1].content[0].image_url.url")
                        .value("data:image/jpeg;base64,/9j/4AAQ"))
                .andExpect(jsonPath("$.messages[1].content[0].originalFileName").doesNotExist())
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiCopywritingResult result = createService(API_KEY).generate(7L, imageInput());

        assertEquals("画面中是一杯放在木桌上的咖啡。", result.imageAnalysis());
        assertEquals("一杯咖啡的治愈时刻", result.title());
        assertEquals("慢下来，享受眼前这一杯咖啡。", result.content());
        assertEquals(List.of("咖啡日常", "生活记录", "治愈时刻"), result.tags());
        mockServer.verify();
    }

    @Test
    void sendsUserImageWebImageAndUrlTextInOneQwenRequest() throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "组合素材分析",
                "title", "组合素材标题",
                "content", "组合素材正文",
                "tags", List.of("组合", "网页", "图片")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content[0].type").value("image_url"))
                .andExpect(jsonPath("$.messages[1].content[1].type").value("image_url"))
                .andExpect(jsonPath("$.messages[1].content[2].type").value("text"))
                .andExpect(jsonPath("$.messages[1].content[2].text")
                        .value(containsString("网页标题：网页标题")))
                .andExpect(jsonPath("$.messages[1].content[2].text")
                        .value(containsString("网页描述：网页描述")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiCopywritingInput input = new AiCopywritingInput(
                imageInfo(),
                imageInfo(),
                "网页标题",
                "网页描述");
        AiCopywritingResult result = createService(API_KEY).generate(9L, input);

        assertEquals("组合素材标题", result.title());
        mockServer.verify();
    }

    @Test
    void rejectsInvalidJsonContent() throws Exception {
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "not-json")))));
        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> createService(API_KEY).generate(7L, imageInput()));

        assertEquals(QwenVisionAiCopywritingService.INVALID_RESPONSE_MESSAGE, exception.getMessage());
        mockServer.verify();
    }

    @Test
    void rejectsResponseWithoutChoices() {
        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> createService(API_KEY).generate(7L, imageInput()));

        assertEquals(QwenVisionAiCopywritingService.INVALID_RESPONSE_MESSAGE, exception.getMessage());
        mockServer.verify();
    }

    @Test
    void convertsUnauthorizedResponseToSafeException() {
        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"provider-sensitive-response\"}"));

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> createService(API_KEY).generate(7L, imageInput()));

        assertEquals(QwenVisionAiCopywritingService.VISION_UNAVAILABLE_MESSAGE, exception.getMessage());
        mockServer.verify();
    }

    @Test
    void convertsServerErrorToSafeException() {
        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"provider-sensitive-response\"}"));

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> createService(API_KEY).generate(7L, imageInput()));

        assertEquals(QwenVisionAiCopywritingService.VISION_UNAVAILABLE_MESSAGE, exception.getMessage());
        mockServer.verify();
    }

    @Test
    void failsSafelyWithoutApiKeyBeforeSendingRequest() {
        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> createService("").generate(7L, imageInput()));

        assertEquals(QwenVisionAiCopywritingService.VISION_UNAVAILABLE_MESSAGE, exception.getMessage());
        mockServer.verify();
    }

    private QwenVisionAiCopywritingService createService(String apiKey) {
        return new QwenVisionAiCopywritingService(
                restClientBuilder,
                objectMapper,
                BASE_URL,
                apiKey,
                MODEL);
    }

    private AiImageInfo imageInfo() {
        return new AiImageInfo(
                "user-file.jpg",
                "stored-image.jpg",
                imagePath.toString(),
                MediaType.IMAGE_JPEG_VALUE,
                6L);
    }

    private AiCopywritingInput imageInput() {
        return new AiCopywritingInput(imageInfo(), null, null, null);
    }
}
