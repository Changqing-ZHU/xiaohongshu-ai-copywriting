package com.example.xhscopywriting.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.dto.CopywritingStyles;
import com.example.xhscopywriting.exception.AiServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QwenVisionAiCopywritingService implements AiCopywritingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            QwenVisionAiCopywritingService.class);

    static final String VISION_UNAVAILABLE_MESSAGE = "Qwen vision model unavailable";
    static final String INVALID_RESPONSE_MESSAGE = "Qwen vision response invalid";
    static final String IMAGE_READ_FAILURE_MESSAGE = "Unable to read image for AI processing";

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp");

    private static final String SYSTEM_PROMPT = """
            你是一名小红书内容创作助手。
            请根据用户提供的一张或多张图片完成内容分析和小红书文案创作。
            必须只返回合法 JSON，不要返回 Markdown 代码块或额外说明。
            JSON 格式必须为：
            {"imageAnalysis":"","title":"","content":"","tags":[]}
            要求：
            1. imageAnalysis 客观总结图片中的真实内容；
            2. title 使用中文、20字以内且有吸引力；
            3. content 使用小红书风格、自然分段，不虚构图片不存在的信息；
            4. tags 返回 3 到 5 个字符串组成的数组。
            """;

    private static final String USER_PROMPT = "请分析提供的素材，并生成符合要求的小红书文案。";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public QwenVisionAiCopywritingService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            String baseUrl,
            String apiKey,
            String model) {
        this.restClient = restClientBuilder.baseUrl(normalizeBaseUrl(baseUrl)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiCopywritingResult generate(Long generationId, AiCopywritingInput input) {
        final Map<String, Object> requestBody;
        try {
            validateConfiguration();
            validateInput(input);
            requestBody = createRequestBody(input);
        } catch (AiServiceException exception) {
            LOGGER.error(
                    "Qwen request preparation failed: generationId={}, reason={}",
                    generationId,
                    exception.getMessage());
            throw exception;
        }

        final String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            LOGGER.error(
                    "Qwen API request failed: generationId={}, httpStatus={}",
                    generationId,
                    exception.getStatusCode().value());
            throw new AiServiceException(VISION_UNAVAILABLE_MESSAGE, exception);
        } catch (RestClientException exception) {
            LOGGER.error(
                    "Qwen API request failed: generationId={}, exceptionType={}",
                    generationId,
                    exception.getClass().getSimpleName());
            throw new AiServiceException(VISION_UNAVAILABLE_MESSAGE, exception);
        }

        try {
            return parseResponse(responseBody);
        } catch (AiServiceException exception) {
            LOGGER.error(
                    "Qwen response validation failed: generationId={}, reason={}",
                    generationId,
                    exception.getMessage());
            throw exception;
        }
    }

    private void validateInput(AiCopywritingInput input) {
        if (input == null || (!input.hasImage() && !input.hasUrlText())) {
            throw new AiServiceException(INVALID_RESPONSE_MESSAGE);
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            throw new AiServiceException(VISION_UNAVAILABLE_MESSAGE);
        }
    }

    private String createDataUrl(AiImageInfo imageInfo) {
        if (imageInfo == null
                || imageInfo.imagePath() == null
                || imageInfo.imagePath().isBlank()
                || !SUPPORTED_CONTENT_TYPES.contains(imageInfo.contentType())) {
            throw new AiServiceException(IMAGE_READ_FAILURE_MESSAGE);
        }

        try {
            Path imagePath = Path.of(imageInfo.imagePath());
            if (!Files.isRegularFile(imagePath)) {
                throw new AiServiceException(IMAGE_READ_FAILURE_MESSAGE);
            }
            byte[] imageBytes = Files.readAllBytes(imagePath);
            return "data:" + imageInfo.contentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AiServiceException aiServiceException) {
                throw aiServiceException;
            }
            throw new AiServiceException(IMAGE_READ_FAILURE_MESSAGE, exception);
        }
    }

    private Map<String, Object> createRequestBody(AiCopywritingInput input) {
        List<Map<String, Object>> userContent = new ArrayList<>();
        addImageContent(userContent, input.userImage());
        addImageContent(userContent, input.urlImage());

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", createUserPrompt(input));
        userContent.add(textContent);

        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", List.copyOf(userContent))),
                "response_format", Map.of("type", "json_object"),
                "stream", false);
    }

    private void addImageContent(List<Map<String, Object>> content, AiImageInfo imageInfo) {
        if (imageInfo == null) {
            return;
        }
        content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", createDataUrl(imageInfo))));
    }

    private String createUserPrompt(AiCopywritingInput input) {
        StringBuilder prompt = new StringBuilder(USER_PROMPT);
        prompt.append("\n文案风格要求：")
                .append(styleInstruction(input.style()));
        if (input.hasUrlText()) {
            prompt.append("\n以下是从网页提取的参考信息：");
            if (input.urlTitle() != null && !input.urlTitle().isBlank()) {
                prompt.append("\n网页标题：").append(input.urlTitle());
            }
            if (input.urlDescription() != null && !input.urlDescription().isBlank()) {
                prompt.append("\n网页描述：").append(input.urlDescription());
            }
        }
        return prompt.toString();
    }

    private String styleInstruction(String style) {
        return switch (CopywritingStyles.normalize(style)) {
            case "recommend" -> "种草推荐。强调核心卖点、真实使用体验和推荐理由，表达有感染力但不过度夸张。";
            case "review" -> "专业测评。关注可观察的特点、优缺点与客观分析，结论清晰，不虚构参数。";
            case "healing" -> "情绪治愈。语气温柔、有共鸣，围绕画面传递舒缓且真诚的情绪。";
            case "minimal" -> "高级简约。语言克制精炼，减少修饰和感叹，突出画面质感与留白。";
            default -> "日常分享。语气自然亲切，像真实记录生活一样轻松、有细节。";
        };
    }

    private AiCopywritingResult parseResponse(String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
            if (!contentNode.isTextual() || contentNode.asText().isBlank()) {
                throw new AiServiceException(INVALID_RESPONSE_MESSAGE);
            }

            JsonNode result = objectMapper.readTree(contentNode.asText());
            String imageAnalysis = requiredText(result, "imageAnalysis");
            String title = requiredText(result, "title");
            String content = requiredText(result, "content");
            JsonNode tagsNode = result.path("tags");
            if (!tagsNode.isArray() || tagsNode.isEmpty()) {
                throw new AiServiceException(INVALID_RESPONSE_MESSAGE);
            }

            List<String> tags = new ArrayList<>();
            for (JsonNode tagNode : tagsNode) {
                if (!tagNode.isTextual() || tagNode.asText().isBlank()) {
                    throw new AiServiceException(INVALID_RESPONSE_MESSAGE);
                }
                tags.add(tagNode.asText());
            }

            return new AiCopywritingResult(imageAnalysis, title, content, List.copyOf(tags));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new AiServiceException(INVALID_RESPONSE_MESSAGE, exception);
        }
    }

    private String requiredText(JsonNode result, String fieldName) {
        JsonNode field = result.path(fieldName);
        if (!field.isTextual() || field.asText().isBlank()) {
            throw new AiServiceException(INVALID_RESPONSE_MESSAGE);
        }
        return field.asText();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new AiServiceException(VISION_UNAVAILABLE_MESSAGE);
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
