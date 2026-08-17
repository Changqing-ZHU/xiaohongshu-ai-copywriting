package com.example.xhscopywriting.dto;

public record GenerationCreateRequest(String url) {

    public GenerationCreateRequest() {
        this(null);
    }
}
