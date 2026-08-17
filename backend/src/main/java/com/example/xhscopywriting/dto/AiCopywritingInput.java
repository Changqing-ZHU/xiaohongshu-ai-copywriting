package com.example.xhscopywriting.dto;

public record AiCopywritingInput(
        AiImageInfo userImage,
        AiImageInfo urlImage,
        String urlTitle,
        String urlDescription,
        String style) {

    public AiCopywritingInput(
            AiImageInfo userImage,
            AiImageInfo urlImage,
            String urlTitle,
            String urlDescription) {
        this(userImage, urlImage, urlTitle, urlDescription, CopywritingStyles.DEFAULT);
    }

    public boolean hasImage() {
        return userImage != null || urlImage != null;
    }

    public boolean hasUrlText() {
        return hasText(urlTitle) || hasText(urlDescription);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
