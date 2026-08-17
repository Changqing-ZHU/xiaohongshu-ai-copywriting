package com.example.xhscopywriting.dto;

import java.util.Set;

public final class CopywritingStyles {

    public static final String DEFAULT = "daily";

    private static final Set<String> SUPPORTED = Set.of(
            DEFAULT,
            "recommend",
            "review",
            "healing",
            "minimal");

    private CopywritingStyles() {
    }

    public static String normalize(String style) {
        if (style == null || style.isBlank()) {
            return DEFAULT;
        }
        String normalized = style.trim().toLowerCase();
        return SUPPORTED.contains(normalized) ? normalized : DEFAULT;
    }
}
