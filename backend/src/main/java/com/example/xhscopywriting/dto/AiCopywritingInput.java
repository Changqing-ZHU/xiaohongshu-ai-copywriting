package com.example.xhscopywriting.dto;

public record AiCopywritingInput(
        AiImageInfo userImage,
        AiImageInfo urlImage,
        String urlTitle,
        String urlDescription,
        CopywritingOptions options) {

    public AiCopywritingInput(
            AiImageInfo userImage,
            AiImageInfo urlImage,
            String urlTitle,
            String urlDescription) {
        this(userImage, urlImage, urlTitle, urlDescription, CopywritingOptions.defaults());
    }

    public AiCopywritingInput(
            AiImageInfo userImage,
            AiImageInfo urlImage,
            String urlTitle,
            String urlDescription,
            String style) {
        this(
                userImage,
                urlImage,
                urlTitle,
                urlDescription,
                new CopywritingOptions(
                        style,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null).normalized());
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
