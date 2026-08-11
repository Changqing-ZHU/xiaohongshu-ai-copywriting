package com.example.xhscopywriting.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.ImageStorageResult;
import com.example.xhscopywriting.exception.ImageStorageException;
import com.example.xhscopywriting.exception.InvalidImageException;

@Service
public class ImageStorageService {

    private final Path uploadDirectory;

    public ImageStorageService(@Value("${app.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public ImageStorageResult store(MultipartFile image) {
        validateBasicFileProperties(image);

        Path destination = null;
        try {
            try (InputStream rawStream = image.getInputStream();
                 BufferedInputStream inputStream = new BufferedInputStream(rawStream)) {
                inputStream.mark(16);
                byte[] header = inputStream.readNBytes(12);
                inputStream.reset();

                ImageType imageType = detectImageType(header);
                String storedFileName = UUID.randomUUID() + imageType.extension;
                destination = uploadDirectory.resolve(storedFileName).normalize();
                if (!destination.startsWith(uploadDirectory)) {
                    throw new InvalidImageException("Invalid image storage path");
                }

                Files.createDirectories(uploadDirectory);
                Files.copy(inputStream, destination);

                return new ImageStorageResult(
                        storedFileName,
                        destination.toString(),
                        imageType.contentType,
                        image.getSize());
            }
        } catch (IOException exception) {
            if (destination != null) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException ignored) {
                    // Preserve the original storage exception.
                }
            }
            throw new ImageStorageException("Failed to store uploaded image", exception);
        }
    }

    public void deleteStoredFile(ImageStorageResult storedImage) {
        Path imagePath = Path.of(storedImage.imagePath()).toAbsolutePath().normalize();
        if (!imagePath.startsWith(uploadDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException ignored) {
            // Best-effort compensation when the database update fails.
        }
    }

    private void validateBasicFileProperties(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException("Image file must not be empty");
        }

    }

    private ImageType detectImageType(byte[] header) {
        if (isJpeg(header)) {
            return ImageType.JPEG;
        }
        if (isPng(header)) {
            return ImageType.PNG;
        }
        if (isWebp(header)) {
            return ImageType.WEBP;
        }
        throw new InvalidImageException("Unsupported or invalid image content");
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (header.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(header[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private enum ImageType {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        WEBP("image/webp", ".webp");

        private final String contentType;
        private final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }
    }
}
