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
import com.example.xhscopywriting.dto.AiCopywritingOptimizationInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.dto.CopywritingStyles;
import com.example.xhscopywriting.dto.CopywritingOptions;
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

    private static final String OPTIMIZATION_PROMPT = """
            请在忠于原图片和原文案事实的前提下，根据用户要求优化小红书文案。
            保留原素材的核心信息，不要虚构图片中不存在的内容。
            返回更新后的 imageAnalysis、title、content 和 tags。
            """;

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

        return executeRequest(generationId, requestBody);
    }

    @Override
    public AiCopywritingResult optimize(
            Long generationId,
            AiCopywritingOptimizationInput input) {
        final Map<String, Object> requestBody;
        try {
            validateConfiguration();
            validateOptimizationInput(input);
            requestBody = createOptimizationRequestBody(input);
        } catch (AiServiceException exception) {
            LOGGER.error(
                    "Qwen optimization request preparation failed: generationId={}, reason={}",
                    generationId,
                    exception.getMessage());
            throw exception;
        }

        return executeRequest(generationId, requestBody);
    }

    private AiCopywritingResult executeRequest(
            Long generationId,
            Map<String, Object> requestBody) {

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

    private void validateOptimizationInput(AiCopywritingOptimizationInput input) {
        if (input == null
                || input.instruction() == null
                || input.instruction().isBlank()
                || input.originalTitle() == null
                || input.originalTitle().isBlank()
                || input.originalContent() == null
                || input.originalContent().isBlank()) {
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

    private Map<String, Object> createOptimizationRequestBody(
            AiCopywritingOptimizationInput input) {
        List<Map<String, Object>> userContent = new ArrayList<>();
        addImageContent(userContent, input.image());
        userContent.add(Map.of(
                "type", "text",
                "text", createOptimizationPrompt(input)));

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
        CopywritingOptions options = input.options() == null
                ? CopywritingOptions.defaults()
                : input.options().normalized();
        StringBuilder prompt = new StringBuilder(USER_PROMPT);
        prompt.append("\n以下创作参数都是必须执行的硬性要求。文案风格决定整体语气和结构，"
                + "不得用通用小红书模板弱化所选风格；最终标题和正文应让读者能明显辨认该风格。");
        prompt.append("\n所选参数：style=").append(options.style())
                .append(", scene=").append(options.scene())
                .append(", audience=").append(options.audience())
                .append(", ageGroup=").append(options.ageGroup())
                .append(", marketingLevel=").append(options.marketingLevel())
                .append(", length=").append(options.length())
                .append(", emojiPreference=").append(options.emojiPreference());
        prompt.append("\n文案风格要求：")
                .append(styleInstruction(options.style()));
        prompt.append("\n内容类型要求：")
                .append(contentTypeInstruction(options.scene()));
        prompt.append("\n目标受众要求：")
                .append(targetAudienceInstruction(options.audience()));
        prompt.append("\n年龄段要求：")
                .append(ageGroupInstruction(options.ageGroup()));
        prompt.append("\n推荐程度要求：")
                .append(recommendationInstruction(options.marketingLevel()));
        prompt.append("\n文案长度要求：")
                .append(copyLengthInstruction(options.length()));
        prompt.append("\nEmoji 使用要求：")
                .append(emojiInstruction(options.emojiPreference()));
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

    private String createOptimizationPrompt(AiCopywritingOptimizationInput input) {
        StringBuilder prompt = new StringBuilder(OPTIMIZATION_PROMPT);
        appendPromptValue(prompt, "原图片分析", input.originalImageAnalysis());
        appendPromptValue(prompt, "原标题", input.originalTitle());
        appendPromptValue(prompt, "原正文", input.originalContent());
        appendPromptValue(prompt, "原标签", input.originalTags());
        appendPromptValue(prompt, "原 URL 标题", input.urlTitle());
        appendPromptValue(prompt, "原 URL 描述", input.urlDescription());
        appendPromptValue(prompt, "用户优化要求", input.instruction());
        return prompt.toString();
    }

    private void appendPromptValue(StringBuilder prompt, String label, String value) {
        if (value != null && !value.isBlank()) {
            prompt.append('\n').append(label).append("：").append(value);
        }
    }

    private String styleInstruction(String style) {
        return switch (CopywritingStyles.normalize(style)) {
            case "recommend" -> "种草推荐：标题必须体现一个具体吸引点；正文按“使用场景—可观察亮点—适合谁—自然推荐理由”展开，语气热情有感染力。不得写成中性记录，也不得虚构功效。";
            case "review" -> "专业测评：正文必须有清晰的测评结构，分别写可观察优点、可能不足、适合人群和客观结论；语气理性克制，避免感叹式种草，不虚构参数。";
            case "healing" -> "情绪治愈：从画面的光线、色彩或氛围切入，多用温柔短句和情绪共鸣；弱化产品卖点与购买引导，不写测评清单。";
            case "minimal" -> "高级简约：使用短句、低饱和词汇和留白感；正文控制段落简洁，少形容词、少感叹号、无网络热梗，不写强营销号召。";
            case "viral" -> "爆款小红书风：标题采用强钩子或反差点；正文第一句立即抓住注意力，并用短段落、节奏变化和明确记忆点推进，结尾形成互动。不得使用虚假数字或夸张承诺。";
            case "authentic" -> "真实体验分享：坚持第一人称，加入基于图片可确认的具体观察和自然感受；允许表达保留意见，避免绝对化结论、营销口号和模板化卖点罗列。";
            case "tutorial" -> "干货攻略风：正文必须按步骤、要点或清单组织，至少给出三个基于素材可支持的实用提示，并包含避坑提醒；减少抒情，不虚构事实。";
            default -> "日常分享：采用第一人称生活记录口吻，从一个具体画面细节切入；自然叙述当下感受，不列卖点、不做专业测评、不使用购买号召。";
        };
    }

    private String contentTypeInstruction(String contentType) {
        return switch (contentType) {
            case "food" -> "美食探店，关注环境、菜品观感、体验亮点和实用探店信息。";
            case "travel" -> "旅行打卡，突出地点氛围、旅途体验和可参考的打卡建议。";
            case "outfit" -> "穿搭分享，关注搭配思路、风格氛围和适用场景。";
            case "product_recommendation" -> "好物推荐，说明可观察的亮点、适用场景和推荐理由。";
            case "product_review" -> "产品测评，兼顾优点、不足、使用感受和适合人群。";
            case "beauty" -> "美妆护肤，关注妆效或使用感，避免虚构功效与不实承诺。";
            case "home" -> "家居生活，突出空间氛围、实用细节和生活方式。";
            case "digital" -> "数码科技，表达清楚、逻辑准确，关注功能体验但不虚构参数。";
            case "learning" -> "学习成长，提炼方法、收获和可执行建议。";
            default -> "日常记录，围绕图片中的真实场景自然分享生活细节。";
        };
    }

    private String targetAudienceInstruction(String targetAudience) {
        return switch (targetAudience) {
            case "students" -> "面向学生党，表达轻松易懂，关注预算、校园场景和实用性。";
            case "young_women" -> "面向年轻女性，表达自然有共鸣，关注审美、体验和生活方式。";
            case "professionals" -> "面向职场人士，表达高效清晰，关注品质、效率和实际价值。";
            case "mothers" -> "面向宝妈群体，表达亲切可靠，关注家庭场景、便利性与真实体验。";
            case "couples" -> "面向情侣用户，适当突出共同体验、互动感和纪念意义。";
            default -> "面向大众用户，避免小众黑话，表达清楚且易于理解。";
        };
    }

    private String ageGroupInstruction(String ageGroup) {
        return switch (ageGroup) {
            case "under_18" -> "适合18岁以下读者，语言活泼健康，避免成人化消费引导。";
            case "18_25" -> "适合18至25岁读者，语气年轻自然，贴近学习、初入职场和社交场景。";
            case "25_35" -> "适合25至35岁读者，兼顾生活品质、实用价值与成熟表达。";
            case "35_plus" -> "适合35岁以上读者，表达稳重清晰，强调品质、可靠性和长期价值。";
            default -> "不限定年龄，使用普适、自然且容易理解的表达。";
        };
    }

    private String recommendationInstruction(String recommendationLevel) {
        return switch (recommendationLevel) {
            case "share" -> "纯分享，不使用购买号召，不刻意突出销售价值。";
            case "strong" -> "明显推荐，清楚表达推荐理由和适合人群，但不夸大。";
            case "marketing" -> "强营销推广，强化卖点与行动引导，同时保持事实准确并避免虚假承诺。";
            default -> "轻度种草，自然融入亮点与推荐理由，避免强烈广告感。";
        };
    }

    private String copyLengthInstruction(String copyLength) {
        return switch (copyLength) {
            case "short" -> "简短版，正文控制在约100至180个中文字符，突出核心信息。";
            case "detailed" -> "详细版，正文约500至800个中文字符，分段充分并包含更多实用细节。";
            default -> "标准版，正文约250至450个中文字符，信息完整且阅读节奏自然。";
        };
    }

    private String emojiInstruction(String emojiPreference) {
        return switch (emojiPreference) {
            case "none" -> "不使用 Emoji。";
            case "rich" -> "丰富使用 Emoji，适度分布在标题、段落和要点中，但不要影响阅读。";
            default -> "少量使用 Emoji，仅用于自然强调重点。";
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
