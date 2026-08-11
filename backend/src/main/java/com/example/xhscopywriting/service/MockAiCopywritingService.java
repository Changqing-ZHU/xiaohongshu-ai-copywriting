package com.example.xhscopywriting.service;

import java.util.List;

import com.example.xhscopywriting.dto.AiCopywritingResult;
import com.example.xhscopywriting.dto.AiImageInfo;

public class MockAiCopywritingService implements AiCopywritingService {

    @Override
    public AiCopywritingResult generate(Long generationId, AiImageInfo imageInfo) {
        return new AiCopywritingResult(
                "模拟图片分析：画面氛围自然，主体清晰，适合分享生活方式内容。",
                "把普通日子过成喜欢的样子 ✨",
                "这是一段由模拟 AI 服务生成的小红书正文，用于验证后端业务流程。",
                List.of("生活记录", "治愈日常", "氛围感"));
    }
}
