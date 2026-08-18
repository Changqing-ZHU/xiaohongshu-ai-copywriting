package com.example.xhscopywriting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;
import com.example.xhscopywriting.dto.CopywritingStyles;
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

    private final GenerationRepository generationRepository;
    private final ImageStorageService imageStorageService;
    private final UrlContentService urlContentService;
    private final AiCopywritingService aiCopywritingService;
    private final Map<Long, String> generationStyles = new ConcurrentHashMap<>();

    public GenerationService(
            GenerationRepository generationRepository,
            ImageStorageService imageStorageService,
            UrlContentService urlContentService,
            AiCopywritingService aiCopywritingService) {
        this.generationRepository = generationRepository;
        this.imageStorageService = imageStorageService;
        this.urlContentService = urlContentService;
        this.aiCopywritingService = aiCopywritingService;
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
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);

        try {
            generationRepository.insert(generation);
            generationStyles.put(generation.getId(), CopywritingStyles.normalize(request.style()));
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

    private Generation processGeneration(Generation generation, AiImageInfo userImage) {
        PreparedUrlInput preparedUrl = null;
        try {
            preparedUrl = prepareUrlInput(generation, userImage);
            AiCopywritingInput aiInput = new AiCopywritingInput(
                    userImage,
                    preparedUrl == null ? null : preparedUrl.imageInfo(),
                    null,
                    null,
                    generationStyles.getOrDefault(
                            generation.getId(),
                            CopywritingStyles.DEFAULT));

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
            generationStyles.remove(generation.getId());
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
