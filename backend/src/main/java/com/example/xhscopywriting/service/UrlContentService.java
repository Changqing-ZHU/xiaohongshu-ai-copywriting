package com.example.xhscopywriting.service;

import com.example.xhscopywriting.dto.DownloadedImage;

public interface UrlContentService {

    DownloadedImage downloadImage(String imageUrl);
}
