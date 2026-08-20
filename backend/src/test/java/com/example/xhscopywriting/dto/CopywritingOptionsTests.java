package com.example.xhscopywriting.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class CopywritingOptionsTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bindsCanonicalGenerationParameterNames() throws Exception {
        GenerationCreateRequest request = objectMapper.readValue("""
                {
                  "style":"review",
                  "scene":"digital",
                  "audience":"professionals",
                  "ageGroup":"25_35",
                  "marketingLevel":"share",
                  "length":"detailed",
                  "emojiPreference":"none"
                }
                """, GenerationCreateRequest.class);

        CopywritingOptions options = CopywritingOptions.from(request);

        assertEquals("review", options.style());
        assertEquals("digital", options.scene());
        assertEquals("professionals", options.audience());
        assertEquals("25_35", options.ageGroup());
        assertEquals("share", options.marketingLevel());
        assertEquals("detailed", options.length());
        assertEquals("none", options.emojiPreference());
    }

    @Test
    void acceptsLegacyNamesButPersistsCanonicalNames() throws Exception {
        GenerationCreateRequest request = objectMapper.readValue("""
                {
                  "contentType":"travel",
                  "targetAudience":"couples",
                  "recommendationLevel":"light",
                  "copyLength":"short"
                }
                """, GenerationCreateRequest.class);

        JsonNode stored = objectMapper.readTree(
                objectMapper.writeValueAsString(CopywritingOptions.from(request)));

        assertEquals("travel", stored.path("scene").asText());
        assertEquals("couples", stored.path("audience").asText());
        assertEquals("light", stored.path("marketingLevel").asText());
        assertEquals("short", stored.path("length").asText());
        assertFalse(stored.has("contentType"));
        assertFalse(stored.has("targetAudience"));
        assertFalse(stored.has("recommendationLevel"));
        assertFalse(stored.has("copyLength"));
    }

    @Test
    void readsPreviouslyPersistedOptionJson() throws Exception {
        CopywritingOptions options = objectMapper.readValue("""
                {
                  "style":"healing",
                  "contentType":"daily_record",
                  "targetAudience":"general",
                  "ageGroup":"unrestricted",
                  "recommendationLevel":"light",
                  "copyLength":"standard",
                  "emojiPreference":"few"
                }
                """, CopywritingOptions.class).normalized();

        assertEquals("healing", options.style());
        assertEquals("daily_record", options.scene());
        assertEquals("general", options.audience());
        assertEquals("light", options.marketingLevel());
        assertEquals("standard", options.length());
    }
}
