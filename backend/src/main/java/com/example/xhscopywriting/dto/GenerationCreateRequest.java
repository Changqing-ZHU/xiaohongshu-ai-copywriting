package com.example.xhscopywriting.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GenerationCreateRequest(
        String url,
        String style,
        @JsonAlias("contentType") String scene,
        @JsonAlias("targetAudience") String audience,
        String ageGroup,
        @JsonAlias("recommendationLevel") String marketingLevel,
        @JsonAlias("copyLength") String length,
        String emojiPreference) {

    public GenerationCreateRequest() {
        this(null, null, null, null, null, null, null, null);
    }

    public GenerationCreateRequest(String url) {
        this(url, null, null, null, null, null, null, null);
    }
}
