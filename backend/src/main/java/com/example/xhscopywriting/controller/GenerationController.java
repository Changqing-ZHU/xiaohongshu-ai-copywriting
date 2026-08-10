package com.example.xhscopywriting.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.dto.GenerationCreatedResponse;
import com.example.xhscopywriting.dto.GenerationImageUploadedResponse;
import com.example.xhscopywriting.dto.GenerationResponse;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.service.GenerationService;

@RestController
@RequestMapping("/api/generations")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public ResponseEntity<GenerationCreatedResponse> createGeneration(
            @RequestBody(required = false) GenerationCreateRequest request) {
        GenerationCreateRequest effectiveRequest = request == null
                ? new GenerationCreateRequest()
                : request;
        Generation generation = generationService.createGeneration(effectiveRequest);
        GenerationCreatedResponse response = new GenerationCreatedResponse(
                generation.getId(),
                generation.getStatus(),
                generation.getCreatedAt());

        return ResponseEntity
                .created(URI.create("/api/generations/" + generation.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenerationResponse> findGenerationById(@PathVariable Long id) {
        Generation generation = generationService.findById(id);
        return ResponseEntity.ok(GenerationResponse.from(generation));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GenerationImageUploadedResponse> uploadImage(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image) {
        Generation generation = generationService.uploadImage(id, image);
        return ResponseEntity.ok(new GenerationImageUploadedResponse(
                generation.getId(),
                generation.getStatus()));
    }
}
