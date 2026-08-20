package com.example.xhscopywriting.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CopywritingOptions(
        String style,
        @JsonAlias("contentType") String scene,
        @JsonAlias("targetAudience") String audience,
        String ageGroup,
        @JsonAlias("recommendationLevel") String marketingLevel,
        @JsonAlias("copyLength") String length,
        String emojiPreference) {

    public static final String DEFAULT_CONTENT_TYPE = "daily_record";
    public static final String DEFAULT_TARGET_AUDIENCE = "general";
    public static final String DEFAULT_AGE_GROUP = "unrestricted";
    public static final String DEFAULT_RECOMMENDATION_LEVEL = "light";
    public static final String DEFAULT_COPY_LENGTH = "standard";
    public static final String DEFAULT_EMOJI_PREFERENCE = "few";

    private static final Set<String> CONTENT_TYPES = Set.of(
            DEFAULT_CONTENT_TYPE,
            "food",
            "travel",
            "outfit",
            "product_recommendation",
            "product_review",
            "beauty",
            "home",
            "digital",
            "learning");
    private static final Set<String> TARGET_AUDIENCES = Set.of(
            "students",
            "young_women",
            "professionals",
            "mothers",
            "couples",
            DEFAULT_TARGET_AUDIENCE);
    private static final Set<String> AGE_GROUPS = Set.of(
            "under_18",
            "18_25",
            "25_35",
            "35_plus",
            DEFAULT_AGE_GROUP);
    private static final Set<String> RECOMMENDATION_LEVELS = Set.of(
            "share",
            DEFAULT_RECOMMENDATION_LEVEL,
            "strong",
            "marketing");
    private static final Set<String> COPY_LENGTHS = Set.of(
            "short",
            DEFAULT_COPY_LENGTH,
            "detailed");
    private static final Set<String> EMOJI_PREFERENCES = Set.of(
            "none",
            DEFAULT_EMOJI_PREFERENCE,
            "rich");

    public static CopywritingOptions from(GenerationCreateRequest request) {
        if (request == null) {
            return defaults();
        }
        return new CopywritingOptions(
                CopywritingStyles.normalize(request.style()),
                normalize(request.scene(), CONTENT_TYPES, DEFAULT_CONTENT_TYPE),
                normalize(request.audience(), TARGET_AUDIENCES, DEFAULT_TARGET_AUDIENCE),
                normalize(request.ageGroup(), AGE_GROUPS, DEFAULT_AGE_GROUP),
                normalize(
                        request.marketingLevel(),
                        RECOMMENDATION_LEVELS,
                        DEFAULT_RECOMMENDATION_LEVEL),
                normalize(request.length(), COPY_LENGTHS, DEFAULT_COPY_LENGTH),
                normalize(
                        request.emojiPreference(),
                        EMOJI_PREFERENCES,
                        DEFAULT_EMOJI_PREFERENCE));
    }

    public static CopywritingOptions defaults() {
        return new CopywritingOptions(
                CopywritingStyles.DEFAULT,
                DEFAULT_CONTENT_TYPE,
                DEFAULT_TARGET_AUDIENCE,
                DEFAULT_AGE_GROUP,
                DEFAULT_RECOMMENDATION_LEVEL,
                DEFAULT_COPY_LENGTH,
                DEFAULT_EMOJI_PREFERENCE);
    }

    public CopywritingOptions normalized() {
        return new CopywritingOptions(
                CopywritingStyles.normalize(style),
                normalize(scene, CONTENT_TYPES, DEFAULT_CONTENT_TYPE),
                normalize(audience, TARGET_AUDIENCES, DEFAULT_TARGET_AUDIENCE),
                normalize(ageGroup, AGE_GROUPS, DEFAULT_AGE_GROUP),
                normalize(
                        marketingLevel,
                        RECOMMENDATION_LEVELS,
                        DEFAULT_RECOMMENDATION_LEVEL),
                normalize(length, COPY_LENGTHS, DEFAULT_COPY_LENGTH),
                normalize(emojiPreference, EMOJI_PREFERENCES, DEFAULT_EMOJI_PREFERENCE));
    }

    private static String normalize(String value, Set<String> supported, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase();
        return supported.contains(normalized) ? normalized : defaultValue;
    }
}
