package com.example.xhscopywriting.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import com.example.xhscopywriting.config.UrlContentProperties;
import com.example.xhscopywriting.dto.DownloadedImage;
import com.example.xhscopywriting.exception.UrlContentException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class UrlContentServiceTests {

    private HttpServer server;
    private DefaultUrlContentService service;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/photo.jpg", exchange -> respond(
                exchange, 200, "image/jpeg", jpegBytes()));
        server.createContext("/photo.png", exchange -> respond(
                exchange, 200, "image/png; charset=binary", pngBytes()));
        server.createContext("/photo.webp", exchange -> respond(
                exchange, 200, "image/webp", webpBytes()));
        server.createContext("/article", exchange -> respond(
                exchange, 200, "text/html; charset=UTF-8",
                "<html><title>Not an image</title></html>".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/failure", exchange -> respond(
                exchange, 500, "text/plain", "failure".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/photo.jpg");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        service = createService(new UrlContentProperties());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void downloadsJpegImageDirectly() {
        DownloadedImage image = service.downloadImage(baseUrl + "/photo.jpg");

        assertEquals("photo.jpg", image.originalFileName());
        assertEquals("image/jpeg", image.contentType());
        assertArrayEquals(jpegBytes(), image.content());
    }

    @Test
    void supportsPngAndWebpContentTypes() {
        DownloadedImage png = service.downloadImage(baseUrl + "/photo.png");
        DownloadedImage webp = service.downloadImage(baseUrl + "/photo.webp");

        assertEquals("image/png", png.contentType());
        assertArrayEquals(pngBytes(), png.content());
        assertEquals("image/webp", webp.contentType());
        assertArrayEquals(webpBytes(), webp.content());
    }

    @Test
    void followsValidatedRedirectToImage() {
        DownloadedImage image = service.downloadImage(baseUrl + "/redirect");

        assertEquals("image/jpeg", image.contentType());
        assertArrayEquals(jpegBytes(), image.content());
    }

    @Test
    void rejectsInvalidOrUnavailableUrlWithSafeAccessMessage() {
        UrlContentException invalid = assertThrows(
                UrlContentException.class,
                () -> service.downloadImage("file:///etc/passwd"));
        UrlContentException unavailable = assertThrows(
                UrlContentException.class,
                () -> service.downloadImage(baseUrl + "/failure"));

        assertEquals(DefaultUrlContentService.SAFE_ACCESS_FAILURE_MESSAGE, invalid.getMessage());
        assertEquals(DefaultUrlContentService.SAFE_ACCESS_FAILURE_MESSAGE, unavailable.getMessage());
    }

    @Test
    void rejectsHtmlResponseAsUnsupportedImageFormat() {
        UrlContentException exception = assertThrows(
                UrlContentException.class,
                () -> service.downloadImage(baseUrl + "/article"));

        assertEquals(DefaultUrlContentService.SAFE_FORMAT_FAILURE_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsImageLargerThanConfiguredLimit() {
        UrlContentProperties properties = new UrlContentProperties();
        properties.setMaxImageSize(DataSize.ofBytes(4));

        UrlContentException exception = assertThrows(
                UrlContentException.class,
                () -> createService(properties).downloadImage(baseUrl + "/photo.jpg"));

        assertEquals(DefaultUrlContentService.SAFE_SIZE_FAILURE_MESSAGE, exception.getMessage());
    }

    private DefaultUrlContentService createService(UrlContentProperties properties) {
        return new DefaultUrlContentService(
                HttpClient.newHttpClient(),
                properties,
                uri -> { });
    }

    private void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private byte[] jpegBytes() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10
        };
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private byte[] webpBytes() {
        return new byte[] {
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
        };
    }
}
