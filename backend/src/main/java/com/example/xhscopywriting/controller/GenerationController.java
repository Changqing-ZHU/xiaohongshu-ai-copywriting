package com.example.xhscopywriting.controller;

import java.net.URI;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.dto.GenerationCreatedResponse;
import com.example.xhscopywriting.dto.GenerationHistoryResponse;
import com.example.xhscopywriting.dto.GenerationImageResource;
import com.example.xhscopywriting.dto.GenerationImageUploadedResponse;
import com.example.xhscopywriting.dto.GenerationOptimizeRequest;
import com.example.xhscopywriting.dto.GenerationProcessingResponse;
import com.example.xhscopywriting.dto.GenerationResponse;
import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.model.User;
import com.example.xhscopywriting.security.CurrentUserService;
import com.example.xhscopywriting.service.GenerationAsyncService;
import com.example.xhscopywriting.service.GenerationImageResourceService;
import com.example.xhscopywriting.service.GenerationService;

@RestController
@RequestMapping("/api/generations")
public class GenerationController {

    private final GenerationService generationService;
    private final GenerationAsyncService generationAsyncService;
    private final GenerationImageResourceService generationImageResourceService;
    private final CurrentUserService currentUserService;

    public GenerationController(
            GenerationService generationService,
            GenerationAsyncService generationAsyncService,
            GenerationImageResourceService generationImageResourceService,
            CurrentUserService currentUserService) {
        this.generationService = generationService;
        this.generationAsyncService = generationAsyncService;
        this.generationImageResourceService = generationImageResourceService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<GenerationProcessingResponse> generateFromStoredInputs(
            @PathVariable Long id) {
        Generation generation = generationService.requireUrlInput(id);
        generationAsyncService.processUrlOnly(id);
        return ResponseEntity.accepted().body(new GenerationProcessingResponse(
                generation.getId(),
                generation.getStatus()));
    }

    @PostMapping
    public ResponseEntity<GenerationCreatedResponse> createGeneration(
            @RequestBody(required = false) GenerationCreateRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        User currentUser = currentUserService.requireUser(authorizationHeader);
        GenerationCreateRequest effectiveRequest = request == null
                ? new GenerationCreateRequest()
                : request;
        Generation generation = generationService.createGeneration(
                effectiveRequest,
                currentUser.getId());
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

    @GetMapping
    public ResponseEntity<List<GenerationHistoryResponse>> findAllGenerations(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        User currentUser = currentUserService.requireUser(authorizationHeader);
        List<GenerationHistoryResponse> response = generationService
                .findAllByUserId(currentUser.getId())
                .stream()
                .map(GenerationHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> findGenerationImage(@PathVariable Long id) {
        GenerationImageResource image = generationImageResourceService.load(id);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .contentLength(image.contentLength())
                .body(image.resource());
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

    @PostMapping("/{id}/optimize")
    public ResponseEntity<GenerationResponse> optimizeGeneration(
            @PathVariable Long id,
            @RequestBody GenerationOptimizeRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        User currentUser = currentUserService.requireUser(authorizationHeader);
        Generation optimized = generationService.optimizeGeneration(
                id,
                currentUser.getId(),
                request == null ? null : request.instruction());
        return ResponseEntity
                .created(URI.create("/api/generations/" + optimized.getId()))
                .body(GenerationResponse.from(optimized));
    }
}
