package com.example.xhscopywriting.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import com.example.xhscopywriting.dto.DownloadedImage;

public class DownloadedMultipartFile implements MultipartFile {

    private final DownloadedImage image;

    public DownloadedMultipartFile(DownloadedImage image) {
        this.image = image;
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getOriginalFilename() {
        return image.originalFileName();
    }

    @Override
    public String getContentType() {
        return image.contentType();
    }

    @Override
    public boolean isEmpty() {
        return image.content().length == 0;
    }

    @Override
    public long getSize() {
        return image.content().length;
    }

    @Override
    public byte[] getBytes() {
        return image.content().clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(image.content());
    }

    @Override
    public void transferTo(File destination) throws IOException {
        Files.write(destination.toPath(), image.content());
    }

    @Override
    public void transferTo(Path destination) throws IOException {
        Files.write(destination, image.content());
    }
}
