package com.example.xhscopywriting.service;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiCopywritingInput;

public interface AiCopywritingService {

    AiCopywritingResult generate(Long generationId, AiCopywritingInput input);
}
