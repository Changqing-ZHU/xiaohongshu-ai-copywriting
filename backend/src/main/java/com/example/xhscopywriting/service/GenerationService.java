package com.example.xhscopywriting.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.dto.ImageStorageResult;
import com.example.xhscopywriting.exception.GenerationCreationException;
import com.example.xhscopywriting.exception.GenerationNotFoundException;
import com.example.xhscopywriting.exception.GenerationProcessingException;
import com.example.xhscopywriting.exception.ImageUploadException;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;

@Service
public class GenerationService {

    private static final String INITIAL_STATUS = "PROCESSING";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String FAILED_STATUS = "FAILED";
    private static final String SAFE_AI_FAILURE_MESSAGE =
            "AI copywriting generation failed. Please try again later.";

    private final GenerationRepository generationRepository;
    private final ImageStorageService imageStorageService;
    private final AiCopywritingService aiCopywritingService;

    public GenerationService(
            GenerationRepository generationRepository,
            ImageStorageService imageStorageService,
            AiCopywritingService aiCopywritingService) {
        this.generationRepository = generationRepository;
        this.imageStorageService = imageStorageService;
        this.aiCopywritingService = aiCopywritingService;
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

    @Transactional(readOnly = true)
    public Generation findById(Long id) {
        return generationRepository.findById(id)
                .orElseThrow(() -> new GenerationNotFoundException(id));
    }

    @Transactional(noRollbackFor = GenerationProcessingException.class)
    public Generation uploadImage(Long id, MultipartFile image) {
        Generation generation = findById(id);
        ImageStorageResult storedImage = imageStorageService.store(image);
        String originalFileName = sanitizeOriginalFileName(image.getOriginalFilename());
        LocalDateTime updatedAt = LocalDateTime.now().withNano(0);

        try {
            int updatedRows = generationRepository.updateImageInfo(
                    id,
                    originalFileName,
                    storedImage.storedFileName(),
                    storedImage.imagePath(),
                    storedImage.contentType(),
                    storedImage.size(),
                    updatedAt);

            if (updatedRows != 1) {
                throw new GenerationNotFoundException(id);
            }
        } catch (GenerationNotFoundException exception) {
            imageStorageService.deleteStoredFile(storedImage);
            throw exception;
        } catch (RuntimeException exception) {
            imageStorageService.deleteStoredFile(storedImage);
            throw new ImageUploadException("Failed to update generation image metadata", exception);
        }

        generation.setOriginalFileName(originalFileName);
        generation.setStoredFileName(storedImage.storedFileName());
        generation.setImagePath(storedImage.imagePath());
        generation.setImageContentType(storedImage.contentType());
        generation.setImageSize(storedImage.size());
        generation.setUpdatedAt(updatedAt);

        AiCopywritingResult aiResult;
        try {
            aiResult = aiCopywritingService.generate(
                    id,
                    new AiImageInfo(
                            originalFileName,
                            storedImage.storedFileName(),
                            storedImage.imagePath(),
                            storedImage.contentType(),
                            storedImage.size()));
        } catch (RuntimeException exception) {
            LocalDateTime failedAt = LocalDateTime.now().withNano(0);
            int updatedRows = generationRepository.markFailed(
                    id,
                    SAFE_AI_FAILURE_MESSAGE,
                    failedAt);
            if (updatedRows != 1) {
                throw new GenerationNotFoundException(id);
            }

            generation.setStatus(FAILED_STATUS);
            generation.setErrorMessage(SAFE_AI_FAILURE_MESSAGE);
            generation.setUpdatedAt(failedAt);
            throw new GenerationProcessingException(SAFE_AI_FAILURE_MESSAGE, exception);
        }

        LocalDateTime completedAt = LocalDateTime.now().withNano(0);
        String storedTags = String.join(",", aiResult.tags());
        int updatedRows = generationRepository.updateGenerationResult(
                id,
                aiResult.imageAnalysis(),
                aiResult.title(),
                aiResult.content(),
                storedTags,
                COMPLETED_STATUS,
                completedAt);

        if (updatedRows != 1) {
            throw new GenerationNotFoundException(id);
        }

        generation.setImageAnalysis(aiResult.imageAnalysis());
        generation.setTitle(aiResult.title());
        generation.setContent(aiResult.content());
        generation.setTags(storedTags);
        generation.setErrorMessage(null);
        generation.setStatus(COMPLETED_STATUS);
        generation.setUpdatedAt(completedAt);
        return generation;
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "image";
        }

        String normalized = originalFileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
