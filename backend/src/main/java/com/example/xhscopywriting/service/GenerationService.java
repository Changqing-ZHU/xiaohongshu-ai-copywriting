package com.example.xhscopywriting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingOptimizationInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.dto.CopywritingOptions;
import com.example.xhscopywriting.dto.DownloadedImage;
import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.dto.ImageStorageResult;
import com.example.xhscopywriting.exception.GenerationCreationException;
import com.example.xhscopywriting.exception.GenerationNotFoundException;
import com.example.xhscopywriting.exception.GenerationProcessingException;
import com.example.xhscopywriting.exception.AiServiceException;
import com.example.xhscopywriting.exception.ImageUploadException;
import com.example.xhscopywriting.exception.InvalidGenerationInputException;
import com.example.xhscopywriting.exception.InvalidImageException;
import com.example.xhscopywriting.exception.UrlContentException;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationService.class);

    private static final String INITIAL_STATUS = "PROCESSING";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String FAILED_STATUS = "FAILED";
    private static final String SAFE_AI_FAILURE_MESSAGE =
            "AI copywriting generation failed. Please try again later.";
    private static final String SAFE_URL_ACCESS_FAILURE_MESSAGE =
            "Unable to access image URL. Please check the URL and try again.";
    private static final String SAFE_URL_FORMAT_FAILURE_MESSAGE =
            "Unsupported image URL format. Please use JPEG, PNG, or WebP.";
    private static final String MISSING_INPUT_MESSAGE = "Please provide an image or URL.";
    private static final String INVALID_OPTIMIZATION_MESSAGE =
            "Please provide an optimization instruction.";
    private static final int MAX_OPTIMIZATION_INSTRUCTION_LENGTH = 500;

    private final GenerationRepository generationRepository;
    private final ImageStorageService imageStorageService;
    private final UrlContentService urlContentService;
    private final AiCopywritingService aiCopywritingService;
    private final ObjectMapper objectMapper;

    public GenerationService(
            GenerationRepository generationRepository,
            ImageStorageService imageStorageService,
            UrlContentService urlContentService,
            AiCopywritingService aiCopywritingService,
            ObjectMapper objectMapper) {
        this.generationRepository = generationRepository;
        this.imageStorageService = imageStorageService;
        this.urlContentService = urlContentService;
        this.aiCopywritingService = aiCopywritingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Generation createGeneration(GenerationCreateRequest request, Long userId) {
        Objects.requireNonNull(request, "Generation create request must not be null");
        Objects.requireNonNull(userId, "Authenticated user id must not be null");

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation generation = new Generation();
        generation.setUserId(userId);
        generation.setStatus(INITIAL_STATUS);
        generation.setSourceUrl(normalizeUrl(request.url()));
        generation.setGenerationOptions(serializeOptions(CopywritingOptions.from(request)));
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

    @Transactional(readOnly = true)
    public List<Generation> findAll() {
        return generationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Generation> findAllByUserId(Long userId) {
        return generationRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Generation requireUrlInput(Long id) {
        Generation generation = findById(id);
        if (isBlank(generation.getSourceUrl())) {
            throw new InvalidGenerationInputException(MISSING_INPUT_MESSAGE);
        }
        return generation;
    }

    @Transactional(noRollbackFor = GenerationProcessingException.class)
    public Generation uploadImage(Long id, MultipartFile image) {
        Generation generation = findById(id);
        ImageStorageResult storedImage = imageStorageService.store(image);
        String originalFileName = sanitizeOriginalFileName(image.getOriginalFilename());
        persistImageInfo(generation, originalFileName, storedImage, true);

        AiImageInfo userImage = toAiImageInfo(originalFileName, storedImage);
        return processGeneration(generation, userImage);
    }

    @Transactional(noRollbackFor = GenerationProcessingException.class)
    public Generation processUrlOnly(Long id) {
        Generation generation = requireUrlInput(id);
        return processGeneration(generation, null);
    }

    @Transactional(noRollbackFor = GenerationProcessingException.class)
    public Generation optimizeGeneration(Long id, Long userId, String instruction) {
        Objects.requireNonNull(userId, "Authenticated user id must not be null");
        Generation original = findById(id);
        if (!userId.equals(original.getUserId())) {
            throw new GenerationNotFoundException(id);
        }
        if (!COMPLETED_STATUS.equals(original.getStatus())
                || isBlank(original.getTitle())
                || isBlank(original.getContent())) {
            throw new InvalidGenerationInputException(
                    "Only completed copywriting can be optimized.");
        }

        String normalizedInstruction = normalizeOptimizationInstruction(instruction);
        Generation optimized = createOptimizationRecord(original);

        AiCopywritingOptimizationInput aiInput = new AiCopywritingOptimizationInput(
                toAiImageInfo(original),
                original.getUrlTitle(),
                original.getUrlDescription(),
                original.getImageAnalysis(),
                original.getTitle(),
                original.getContent(),
                original.getTags(),
                normalizedInstruction);

        final AiCopywritingResult aiResult;
        try {
            aiResult = aiCopywritingService.optimize(optimized.getId(), aiInput);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "AI optimization failed: generationId={}, sourceGenerationId={}, exceptionType={}, reason={}",
                    optimized.getId(),
                    original.getId(),
                    exception.getClass().getSimpleName(),
                    safeAiReason(exception));
            markFailedAndThrow(optimized, SAFE_AI_FAILURE_MESSAGE, exception);
            throw exception;
        }

        LocalDateTime completedAt = LocalDateTime.now().withNano(0);
        String storedTags = String.join(",", aiResult.tags());
        int updatedRows = generationRepository.updateGenerationResult(
                optimized.getId(),
                aiResult.imageAnalysis(),
                aiResult.title(),
                aiResult.content(),
                storedTags,
                COMPLETED_STATUS,
                completedAt);
        if (updatedRows != 1) {
            throw new GenerationNotFoundException(optimized.getId());
        }

        optimized.setImageAnalysis(aiResult.imageAnalysis());
        optimized.setTitle(aiResult.title());
        optimized.setContent(aiResult.content());
        optimized.setTags(storedTags);
        optimized.setStatus(COMPLETED_STATUS);
        optimized.setUpdatedAt(completedAt);
        return optimized;
    }

    private Generation processGeneration(Generation generation, AiImageInfo userImage) {
        PreparedUrlInput preparedUrl = null;
        try {
            preparedUrl = prepareUrlInput(generation, userImage);
            AiCopywritingInput aiInput = new AiCopywritingInput(
                    userImage,
                    preparedUrl == null ? null : preparedUrl.imageInfo(),
                    null,
                    null,
                    deserializeOptions(generation.getGenerationOptions()));

            if (!aiInput.hasImage() && !aiInput.hasUrlText()) {
                markFailedAndThrow(generation, SAFE_URL_ACCESS_FAILURE_MESSAGE, null);
            }

            AiCopywritingResult aiResult;
            try {
                aiResult = aiCopywritingService.generate(generation.getId(), aiInput);
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "AI generation failed: generationId={}, exceptionType={}, reason={}",
                        generation.getId(),
                        exception.getClass().getSimpleName(),
                        safeAiReason(exception));
                markFailedAndThrow(generation, SAFE_AI_FAILURE_MESSAGE, exception);
                throw exception;
            }

            LocalDateTime completedAt = LocalDateTime.now().withNano(0);
            String storedTags = String.join(",", aiResult.tags());
            int updatedRows = generationRepository.updateGenerationResult(
                    generation.getId(),
                    aiResult.imageAnalysis(),
                    aiResult.title(),
                    aiResult.content(),
                    storedTags,
                    COMPLETED_STATUS,
                    completedAt);
            if (updatedRows != 1) {
                throw new GenerationNotFoundException(generation.getId());
            }

            generation.setImageAnalysis(aiResult.imageAnalysis());
            generation.setTitle(aiResult.title());
            generation.setContent(aiResult.content());
            generation.setTags(storedTags);
            generation.setErrorMessage(null);
            generation.setStatus(COMPLETED_STATUS);
            generation.setUpdatedAt(completedAt);
            return generation;
        } finally {
            if (preparedUrl != null && preparedUrl.temporaryStoredImage() != null) {
                imageStorageService.deleteStoredFile(preparedUrl.temporaryStoredImage());
            }
        }
    }

    private PreparedUrlInput prepareUrlInput(Generation generation, AiImageInfo userImage) {
        if (isBlank(generation.getSourceUrl())) {
            return null;
        }

        try {
            DownloadedImage downloadedImage = urlContentService.downloadImage(generation.getSourceUrl());
            DownloadedMultipartFile multipartFile = new DownloadedMultipartFile(downloadedImage);
            ImageStorageResult storedImage = imageStorageService.store(multipartFile);
            String originalFileName = sanitizeOriginalFileName(downloadedImage.originalFileName());
            AiImageInfo imageInfo = toAiImageInfo(originalFileName, storedImage);

            if (userImage == null) {
                persistImageInfo(generation, originalFileName, storedImage, true);
                return new PreparedUrlInput(imageInfo, null);
            }
            return new PreparedUrlInput(imageInfo, storedImage);
        } catch (UrlContentException exception) {
            if (userImage != null) {
                return null;
            }
            markFailedAndThrow(generation, exception.getMessage(), exception);
            throw exception;
        } catch (InvalidImageException exception) {
            if (userImage != null) {
                return null;
            }
            markFailedAndThrow(generation, SAFE_URL_FORMAT_FAILURE_MESSAGE, exception);
            throw exception;
        } catch (RuntimeException exception) {
            if (userImage != null) {
                return null;
            }
            markFailedAndThrow(generation, SAFE_URL_ACCESS_FAILURE_MESSAGE, exception);
            throw exception;
        }
    }

    private void persistImageInfo(
            Generation generation,
            String originalFileName,
            ImageStorageResult storedImage,
            boolean deleteOnFailure) {
        LocalDateTime updatedAt = LocalDateTime.now().withNano(0);
        try {
            int updatedRows = generationRepository.updateImageInfo(
                    generation.getId(),
                    originalFileName,
                    storedImage.storedFileName(),
                    storedImage.imagePath(),
                    storedImage.contentType(),
                    storedImage.size(),
                    updatedAt);
            if (updatedRows != 1) {
                throw new GenerationNotFoundException(generation.getId());
            }
        } catch (GenerationNotFoundException exception) {
            if (deleteOnFailure) {
                imageStorageService.deleteStoredFile(storedImage);
            }
            throw exception;
        } catch (RuntimeException exception) {
            if (deleteOnFailure) {
                imageStorageService.deleteStoredFile(storedImage);
            }
            throw new ImageUploadException("Failed to update generation image metadata", exception);
        }

        generation.setOriginalFileName(originalFileName);
        generation.setStoredFileName(storedImage.storedFileName());
        generation.setImagePath(storedImage.imagePath());
        generation.setImageContentType(storedImage.contentType());
        generation.setImageSize(storedImage.size());
        generation.setUpdatedAt(updatedAt);
    }

    private void markFailedAndThrow(
            Generation generation,
            String safeMessage,
            Throwable cause) {
        LocalDateTime failedAt = LocalDateTime.now().withNano(0);
        int updatedRows = generationRepository.markFailed(
                generation.getId(),
                safeMessage,
                failedAt);
        if (updatedRows != 1) {
            throw new GenerationNotFoundException(generation.getId());
        }

        generation.setStatus(FAILED_STATUS);
        generation.setErrorMessage(safeMessage);
        generation.setUpdatedAt(failedAt);
        throw new GenerationProcessingException(safeMessage, cause);
    }

    private AiImageInfo toAiImageInfo(
            String originalFileName,
            ImageStorageResult storedImage) {
        return new AiImageInfo(
                originalFileName,
                storedImage.storedFileName(),
                storedImage.imagePath(),
                storedImage.contentType(),
                storedImage.size());
    }

    private AiImageInfo toAiImageInfo(Generation generation) {
        if (isBlank(generation.getImagePath())
                || isBlank(generation.getImageContentType())) {
            return null;
        }
        return new AiImageInfo(
                generation.getOriginalFileName(),
                generation.getStoredFileName(),
                generation.getImagePath(),
                generation.getImageContentType(),
                generation.getImageSize() == null ? 0L : generation.getImageSize());
    }

    private Generation createOptimizationRecord(Generation original) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Generation optimized = new Generation();
        optimized.setUserId(original.getUserId());
        optimized.setStatus(INITIAL_STATUS);
        optimized.setSourceUrl(original.getSourceUrl());
        optimized.setUrlTitle(original.getUrlTitle());
        optimized.setUrlDescription(original.getUrlDescription());
        optimized.setGenerationOptions(original.getGenerationOptions());
        optimized.setOriginalFileName(original.getOriginalFileName());
        optimized.setStoredFileName(original.getStoredFileName());
        optimized.setImagePath(original.getImagePath());
        optimized.setImageContentType(original.getImageContentType());
        optimized.setImageSize(original.getImageSize());
        optimized.setCreatedAt(now);
        optimized.setUpdatedAt(now);
        try {
            generationRepository.insert(optimized);
            return optimized;
        } catch (DataAccessException | IllegalStateException exception) {
            throw new GenerationCreationException(
                    "Failed to persist optimized generation task",
                    exception);
        }
    }

    private String normalizeOptimizationInstruction(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            throw new InvalidGenerationInputException(INVALID_OPTIMIZATION_MESSAGE);
        }
        String normalized = instruction.trim();
        if (normalized.length() > MAX_OPTIMIZATION_INSTRUCTION_LENGTH) {
            throw new InvalidGenerationInputException(
                    "Optimization instruction must not exceed 500 characters.");
        }
        return normalized;
    }

    private String serializeOptions(CopywritingOptions options) {
        try {
            return objectMapper.writeValueAsString(options.normalized());
        } catch (JsonProcessingException exception) {
            throw new GenerationCreationException(
                    "Failed to serialize generation options",
                    exception);
        }
    }

    private CopywritingOptions deserializeOptions(String storedOptions) {
        if (storedOptions == null || storedOptions.isBlank()) {
            return CopywritingOptions.defaults();
        }
        try {
            return objectMapper
                    .readValue(storedOptions, CopywritingOptions.class)
                    .normalized();
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Invalid stored generation options; using defaults");
            return CopywritingOptions.defaults();
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.trim();
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "image";
        }
        String normalized = originalFileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeAiReason(RuntimeException exception) {
        String message = exception instanceof AiServiceException
                ? exception.getMessage()
                : null;
        return message == null || message.isBlank()
                ? "unspecified AI service failure"
                : message;
    }

    private record PreparedUrlInput(
            AiImageInfo imageInfo,
            ImageStorageResult temporaryStoredImage) {
    }
}
