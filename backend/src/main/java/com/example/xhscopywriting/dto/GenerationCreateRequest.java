package com.example.xhscopywriting.dto;

public record GenerationCreateRequest(String url, String style) {

    public GenerationCreateRequest() {
        this(null, null);
    }

    public GenerationCreateRequest(String url) {
        this(url, null);
    }
}
