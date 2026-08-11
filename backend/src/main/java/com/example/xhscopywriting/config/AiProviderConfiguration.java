package com.example.xhscopywriting.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.example.xhscopywriting.exception.AiConfigurationException;
import com.example.xhscopywriting.service.AiCopywritingService;
import com.example.xhscopywriting.service.MockAiCopywritingService;
import com.example.xhscopywriting.service.QwenVisionAiCopywritingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderConfiguration {

    @Bean
    public AiCopywritingService aiCopywritingService(
            AiProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        String provider = properties.getProvider();

        if ("mock".equals(provider)) {
            return new MockAiCopywritingService();
        }

        if ("qwen".equals(provider)) {
            AiProperties.Qwen qwen = properties.getQwen();
            requireQwenConfiguration(qwen);
            return new QwenVisionAiCopywritingService(
                    restClientBuilder,
                    objectMapper,
                    qwen.getBaseUrl(),
                    qwen.getApiKey(),
                    qwen.getModel());
        }

        throw new AiConfigurationException("Unsupported AI provider: " + provider);
    }

    private void requireQwenConfiguration(AiProperties.Qwen qwen) {
        if (qwen.getApiKey() == null || qwen.getApiKey().isBlank()) {
            throw new AiConfigurationException(
                    "Qwen API key is required when provider is qwen");
        }
        if (qwen.getModel() == null || qwen.getModel().isBlank()) {
            throw new AiConfigurationException(
                    "Qwen model is required when provider is qwen");
        }
    }
}
