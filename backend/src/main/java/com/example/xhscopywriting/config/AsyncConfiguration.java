package com.example.xhscopywriting.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration(proxyBeanMethods = false)
public class AsyncConfiguration {

    @Bean(name = "urlGenerationExecutor")
    public Executor urlGenerationExecutor(
            @Value("${generation.async.core-pool-size:2}") int corePoolSize,
            @Value("${generation.async.max-pool-size:4}") int maxPoolSize,
            @Value("${generation.async.queue-capacity:50}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("url-generation-");
        executor.initialize();
        return executor;
    }
}
