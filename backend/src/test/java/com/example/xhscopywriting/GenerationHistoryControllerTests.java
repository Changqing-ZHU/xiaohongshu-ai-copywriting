package com.example.xhscopywriting;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.service.GenerationAsyncService;
import com.example.xhscopywriting.service.GenerationImageResourceService;
import com.example.xhscopywriting.service.GenerationService;

@WebMvcTest
class GenerationHistoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerationService generationService;

    @MockBean
    private GenerationAsyncService generationAsyncService;

    @MockBean
    private GenerationImageResourceService generationImageResourceService;

    @Test
    void returnsEmptyHistoryList() throws Exception {
        when(generationService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/generations"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returnsCompletedGenerationInHistoryList() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 17, 14, 30);
        Generation generation = new Generation();
        generation.setId(42L);
        generation.setStatus("COMPLETED");
        generation.setImagePath("uploads/image.jpg");
        generation.setTitle("测试标题");
        generation.setContent("测试正文");
        generation.setTags("穿搭,夏日,灵感");
        generation.setCreatedAt(createdAt);
        when(generationService.findAll()).thenReturn(List.of(generation));

        mockMvc.perform(get("/api/generations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].imageUrl")
                        .value("/api/generations/42/image"))
                .andExpect(jsonPath("$[0].title").value("测试标题"))
                .andExpect(jsonPath("$[0].content").value("测试正文"))
                .andExpect(jsonPath("$[0].tags[0]").value("穿搭"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-08-17T14:30:00"));
    }
}
