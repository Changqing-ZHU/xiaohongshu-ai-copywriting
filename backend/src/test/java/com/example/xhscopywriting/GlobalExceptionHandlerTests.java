package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.example.xhscopywriting.exception.ApiErrorResponse;
import com.example.xhscopywriting.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

class GlobalExceptionHandlerTests {

    @Test
    void returnsSafeMessageWhenUploadExceedsTenMegabytes() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(10L * 1024 * 1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "Image size exceeds limit. Please upload an image smaller than 10MB.",
                response.getBody().message());

        String responseJson = new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(response.getBody());
        assertFalse(responseJson.contains("MaxUploadSizeExceededException"));
        assertFalse(responseJson.contains("stackTrace"));
    }
}
