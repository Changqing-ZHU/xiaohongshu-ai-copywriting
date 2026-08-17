package com.example.xhscopywriting.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.example.xhscopywriting.dto.GenerationImageResource;
import com.example.xhscopywriting.exception.GenerationImageNotFoundException;
import com.example.xhscopywriting.model.Generation;

@Service
public class GenerationImageResourceService {

    private final GenerationService generationService;
    private final Path uploadDirectory;

    public GenerationImageResourceService(
            GenerationService generationService,
            @Value("${app.upload-dir}") String uploadDirectory) {
        this.generationService = generationService;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public GenerationImageResource load(Long generationId) {
        Generation generation = generationService.findById(generationId);
        if (generation.getImagePath() == null || generation.getImagePath().isBlank()) {
            throw new GenerationImageNotFoundException(generationId);
        }

        Path imagePath = Path.of(generation.getImagePath()).toAbsolutePath().normalize();
        if (!imagePath.startsWith(uploadDirectory) || !Files.isRegularFile(imagePath)) {
            throw new GenerationImageNotFoundException(generationId);
        }

        try {
            return new GenerationImageResource(
                    new FileSystemResource(imagePath),
                    supportedMediaType(generation.getImageContentType(), generationId),
                    Files.size(imagePath));
        } catch (IOException exception) {
            throw new GenerationImageNotFoundException(generationId);
        }
    }

    private MediaType supportedMediaType(String contentType, Long generationId) {
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            return MediaType.IMAGE_JPEG;
        }
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            return MediaType.IMAGE_PNG;
        }
        if ("image/webp".equals(contentType)) {
            return MediaType.parseMediaType("image/webp");
        }
        throw new GenerationImageNotFoundException(generationId);
    }
}
