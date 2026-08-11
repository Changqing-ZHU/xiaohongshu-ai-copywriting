package com.example.xhscopywriting.service;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;

public interface AiCopywritingService {

    AiCopywritingResult generate(Long generationId, AiImageInfo imageInfo);
}
