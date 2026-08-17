package com.example.xhscopywriting.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class GenerationAsyncService {

    private final GenerationService generationService;
    private final Set<Long> activeGenerationIds = ConcurrentHashMap.newKeySet();

    public GenerationAsyncService(GenerationService generationService) {
        this.generationService = generationService;
    }

    @Async("urlGenerationExecutor")
    public void processUrlOnly(Long id) {
        if (!activeGenerationIds.add(id)) {
            return;
        }
        try {
            generationService.processUrlOnly(id);
        } catch (RuntimeException ignored) {
            // GenerationService persists a safe FAILED state for query clients.
        } finally {
            activeGenerationIds.remove(id);
        }
    }
}
