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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingOptimizationInput;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.dto.CopywritingOptions;
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
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("日常分享")))
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
    void addsSelectedCopywritingStyleToPrompt() throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "商品图片分析",
                "title", "真实体验分享",
                "content", "这是基于使用体验生成的正文。",
                "tags", List.of("使用体验", "好物分享", "种草")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("种草推荐")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("使用场景—可观察亮点—适合谁—自然推荐理由")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiCopywritingInput input = new AiCopywritingInput(
                imageInfo(),
                null,
                null,
                null,
                "recommend");
        AiCopywritingResult result = createService(API_KEY).generate(11L, input);

        assertEquals("真实体验分享", result.title());
        mockServer.verify();
    }

    @Test
    void supportsDifferentCopywritingStyleInRequest() throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "产品细节分析",
                "title", "客观测评结果",
                "content", "从优缺点出发进行客观分析。",
                "tags", List.of("专业测评", "优缺点", "真实体验")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("专业测评")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("测评结构")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("可能不足")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiCopywritingInput input = new AiCopywritingInput(
                imageInfo(),
                null,
                null,
                null,
                "review");
        AiCopywritingResult result = createService(API_KEY).generate(12L, input);

        assertEquals("客观测评结果", result.title());
        mockServer.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "daily,不列卖点",
            "recommend,使用场景—可观察亮点—适合谁—自然推荐理由",
            "review,可能不足",
            "healing,温柔短句",
            "minimal,无网络热梗"
    })
    void givesEachPrimaryStyleDistinctMandatoryPromptRules(
            String style,
            String expectedRule) throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "图片内容分析",
                "title", "测试标题",
                "content", "测试正文",
                "tags", List.of("测试", "风格", "图片")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("style=" + style)))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString(expectedRule)))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        createService(API_KEY).generate(15L, new AiCopywritingInput(
                imageInfo(),
                null,
                null,
                null,
                style));

        mockServer.verify();
    }

    @Test
    void addsAllEnhancedGenerationOptionsToPrompt() throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "餐厅中的一道菜",
                "title", "这家店真的值得冲",
                "content", "完整探店体验。",
                "tags", List.of("美食探店", "约会餐厅", "真实体验")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("爆款小红书风")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("style=viral, scene=food, audience=young_women")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("不得用通用小红书模板弱化所选风格")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("美食探店")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("年轻女性")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("18至25岁")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("明显推荐")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("详细版")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("丰富使用 Emoji")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiCopywritingInput input = new AiCopywritingInput(
                imageInfo(),
                null,
                null,
                null,
                new CopywritingOptions(
                        "viral",
                        "food",
                        "young_women",
                        "18_25",
                        "strong",
                        "detailed",
                        "rich"));
        AiCopywritingResult result = createService(API_KEY).generate(14L, input);

        assertEquals("这家店真的值得冲", result.title());
        mockServer.verify();
    }

    @Test
    void sendsOriginalCopywritingAndInstructionForOptimization() throws Exception {
        String resultJson = objectMapper.writeValueAsString(Map.of(
                "imageAnalysis", "仍然是一杯咖啡。",
                "title", "和朋友喝杯咖啡",
                "content", "今天和朋友随意聊聊，手边这杯咖啡刚刚好。",
                "tags", List.of("咖啡日常", "朋友相聚", "轻松分享")));
        String providerResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", resultJson)))));

        mockServer.expect(once(), requestTo(BASE_URL + "/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content[0].type").value("image_url"))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("原正文：原来的正式正文")))
                .andExpect(jsonPath("$.messages[1].content[1].text")
                        .value(containsString("用户优化要求：像朋友分享一样")))
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiCopywritingOptimizationInput input = new AiCopywritingOptimizationInput(
                imageInfo(),
                null,
                null,
                "一杯咖啡",
                "原标题",
                "原来的正式正文",
                "咖啡,生活",
                "像朋友分享一样");
        AiCopywritingResult result = createService(API_KEY).optimize(13L, input);

        assertEquals("和朋友喝杯咖啡", result.title());
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
