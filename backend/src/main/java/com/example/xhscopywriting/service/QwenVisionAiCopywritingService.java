package com.example.xhscopywriting.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.exception.AiServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QwenVisionAiCopywritingService implements AiCopywritingService {

    static final String VISION_UNAVAILABLE_MESSAGE = "Qwen vision model unavailable";
    static final String INVALID_RESPONSE_MESSAGE = "Qwen vision response invalid";
    static final String IMAGE_READ_FAILURE_MESSAGE = "Unable to read image for AI processing";

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp");

    private static final String SYSTEM_PROMPT = """
            你是一名小红书内容创作助手。
            请根据用户上传的图片内容完成图片分析和小红书文案创作。
            必须只返回合法 JSON，不要返回 Markdown 代码块或额外说明。
            JSON 格式必须为：
            {"imageAnalysis":"","title":"","content":"","tags":[]}
            要求：
            1. imageAnalysis 描述真实看到的图片内容，不编造不存在的元素；
            2. title 使用中文、20字以内且有吸引力；
            3. content 使用小红书风格、自然分段，不虚构图片不存在的信息；
            4. tags 返回 3 到 5 个字符串组成的数组。
            """;

    private static final String USER_PROMPT = "请分析这张图片，并生成符合要求的小红书文案。";

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
    public AiCopywritingResult generate(Long generationId, AiImageInfo imageInfo) {
        validateConfiguration();
        String dataUrl = createDataUrl(imageInfo);
        Map<String, Object> requestBody = createRequestBody(dataUrl);

        final String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException exception) {
            // Do not expose provider response bodies, credentials, or local paths.
            throw new AiServiceException(VISION_UNAVAILABLE_MESSAGE, exception);
        }

        return parseResponse(responseBody);
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

    private Map<String, Object> createRequestBody(String dataUrl) {
        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl));
        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", USER_PROMPT);

        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", List.of(imageContent, textContent))),
                "response_format", Map.of("type", "json_object"),
                "stream", false);
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
