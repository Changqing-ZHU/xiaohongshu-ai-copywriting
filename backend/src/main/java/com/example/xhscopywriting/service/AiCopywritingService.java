package com.example.xhscopywriting.service;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingOptimizationInput;

public interface AiCopywritingService {

    AiCopywritingResult generate(Long generationId, AiCopywritingInput input);

    AiCopywritingResult optimize(Long generationId, AiCopywritingOptimizationInput input);
}
