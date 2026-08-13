package com.example.xhscopywriting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ai.provider=mock",
                "app.upload-dir=target/test-size-limit-uploads"
        })
class MultipartUploadLimitIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void rejectsMultipartRequestLargerThanTenMegabytesWithSafeJson() throws Exception {
        byte[] oversizedImage = new byte[10 * 1024 * 1024];
        oversizedImage[0] = (byte) 0xFF;
        oversizedImage[1] = (byte) 0xD8;
        oversizedImage[2] = (byte) 0xFF;

        String boundary = "XhsUploadLimitBoundary";
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image\"; filename=\"oversized.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] requestBody = new byte[prefix.length + oversizedImage.length + suffix.length];
        System.arraycopy(prefix, 0, requestBody, 0, prefix.length);
        System.arraycopy(oversizedImage, 0, requestBody, prefix.length, oversizedImage.length);
        System.arraycopy(suffix, 0, requestBody, prefix.length + oversizedImage.length, suffix.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/generations/1/image"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(413, response.statusCode());
        Map<String, Object> responseBody = new ObjectMapper().findAndRegisterModules().readValue(
                response.body(),
                new TypeReference<>() { });
        assertEquals(
                "Image size exceeds limit. Please upload an image smaller than 10MB.",
                responseBody.get("message"));
        assertFalse(responseBody.containsKey("exception"));
        assertFalse(responseBody.containsKey("trace"));
    }
}
