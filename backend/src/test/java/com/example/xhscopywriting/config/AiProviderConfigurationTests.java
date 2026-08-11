package com.example.xhscopywriting.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import com.example.xhscopywriting.exception.AiConfigurationException;
import com.example.xhscopywriting.service.AiCopywritingService;
import com.example.xhscopywriting.service.MockAiCopywritingService;
import com.example.xhscopywriting.service.QwenVisionAiCopywritingService;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiProviderConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiProviderConfiguration.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void defaultsToMockProviderWhenProviderIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiCopywritingService.class);
            assertThat(context.getBean(AiCopywritingService.class))
                    .isInstanceOf(MockAiCopywritingService.class);
        });
    }

    @Test
    void selectsMockProviderExplicitly() {
        contextRunner
                .withPropertyValues("ai.provider=mock")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiCopywritingService.class);
                    assertThat(context.getBean(AiCopywritingService.class))
                            .isInstanceOf(MockAiCopywritingService.class);
                });
    }

    @Test
    void selectsQwenProviderWhenRequiredConfigurationExists() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=qwen",
                        "ai.qwen.api-key=test-key",
                        "ai.qwen.model=confirmed-vision-model")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiCopywritingService.class);
                    assertThat(context.getBean(AiCopywritingService.class))
                            .isInstanceOf(QwenVisionAiCopywritingService.class);
                });
    }

    @Test
    void failsStartupWhenQwenApiKeyIsMissing() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=qwen",
                        "ai.qwen.model=confirmed-vision-model")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(AiConfigurationException.class)
                        .hasRootCauseMessage(
                                "Qwen API key is required when provider is qwen"));
    }

    @Test
    void failsStartupWhenQwenModelIsMissing() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=qwen",
                        "ai.qwen.api-key=test-key")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(AiConfigurationException.class)
                        .hasRootCauseMessage(
                                "Qwen model is required when provider is qwen"));
    }

    @Test
    void failsStartupForUnsupportedProvider() {
        contextRunner
                .withPropertyValues("ai.provider=unknown")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(AiConfigurationException.class)
                        .hasRootCauseMessage("Unsupported AI provider: unknown"));
    }
}
