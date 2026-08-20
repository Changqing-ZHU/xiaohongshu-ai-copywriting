package com.example.xhscopywriting.service;

import java.util.List;

import com.example.xhscopywriting.dto.AiCopywritingInput;
import com.example.xhscopywriting.dto.AiCopywritingOptimizationInput;
import com.example.xhscopywriting.dto.AiCopywritingResult;

public class MockAiCopywritingService implements AiCopywritingService {

    @Override
    public AiCopywritingResult generate(Long generationId, AiCopywritingInput input) {
        return new AiCopywritingResult(
                "模拟图片分析：画面氛围自然，主体清晰，适合分享生活方式内容。",
                "把普通日子过成喜欢的样子 ✨",
                "这是一段由模拟 AI 服务生成的小红书正文，用于验证后端业务流程。",
                List.of("生活记录", "治愈日常", "氛围感"));
    }

    @Override
    public AiCopywritingResult optimize(
            Long generationId,
            AiCopywritingOptimizationInput input) {
        return new AiCopywritingResult(
                input.originalImageAnalysis(),
                "优化后的自然分享 ✨",
                "这是根据你的要求优化后的模拟小红书正文。",
                List.of("自然分享", "生活灵感", "轻松表达"));
    }
}
