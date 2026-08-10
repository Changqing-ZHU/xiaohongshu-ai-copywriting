package com.example.xhscopywriting.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.dto.ImageStorageResult;
import com.example.xhscopywriting.exception.GenerationCreationException;
import com.example.xhscopywriting.exception.GenerationNotFoundException;
import com.example.xhscopywriting.exception.ImageUploadException;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;

@Service
public class GenerationService {

    private static final String INITIAL_STATUS = "PROCESSING";

    private final GenerationRepository generationRepository;
    private final ImageStorageService imageStorageService;

    public GenerationService(
            GenerationRepository generationRepository,
            ImageStorageService imageStorageService) {
        this.generationRepository = generationRepository;
        this.imageStorageService = imageStorageService;
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

    @Transactional
    public Generation uploadImage(Long id, MultipartFile image) {
        Generation generation = findById(id);
        ImageStorageResult storedImage = imageStorageService.store(image);
        LocalDateTime updatedAt = LocalDateTime.now().withNano(0);

        try {
            int updatedRows = generationRepository.updateImageInfo(
                    id,
                    sanitizeOriginalFileName(image.getOriginalFilename()),
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

        generation.setOriginalFileName(sanitizeOriginalFileName(image.getOriginalFilename()));
        generation.setStoredFileName(storedImage.storedFileName());
        generation.setImagePath(storedImage.imagePath());
        generation.setImageContentType(storedImage.contentType());
        generation.setImageSize(storedImage.size());
        generation.setUpdatedAt(updatedAt);
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
