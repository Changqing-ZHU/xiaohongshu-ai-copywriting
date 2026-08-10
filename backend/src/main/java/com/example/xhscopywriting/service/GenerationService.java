package com.example.xhscopywriting.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.exception.GenerationCreationException;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;

@Service
public class GenerationService {

    private static final String INITIAL_STATUS = "PROCESSING";

    private final GenerationRepository generationRepository;

    public GenerationService(GenerationRepository generationRepository) {
        this.generationRepository = generationRepository;
    }

    @Transactional
    public Generation createGeneration(GenerationCreateRequest request) {
        Objects.requireNonNull(request, "Generation create request must not be null");

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation generation = new Generation();
        generation.setStatus(INITIAL_STATUS);
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);

        try {
            generationRepository.insert(generation);
            return generation;
        } catch (DataAccessException | IllegalStateException exception) {
            throw new GenerationCreationException("Failed to persist generation task", exception);
        }
    }
}
