package com.example.xhscopywriting.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.xhscopywriting.dto.GenerationCreateRequest;
import com.example.xhscopywriting.dto.GenerationCreatedResponse;
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
}
