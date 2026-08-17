package com.example.xhscopywriting.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.xhscopywriting.config.UrlContentProperties;
import com.example.xhscopywriting.dto.DownloadedImage;
import com.example.xhscopywriting.exception.UrlContentException;

@Service
public class DefaultUrlContentService implements UrlContentService {

    static final String SAFE_ACCESS_FAILURE_MESSAGE =
            "Unable to access image URL. Please check the URL and try again.";
    static final String SAFE_FORMAT_FAILURE_MESSAGE =
            "Unsupported image URL format. Please use JPEG, PNG, or WebP.";
    static final String SAFE_SIZE_FAILURE_MESSAGE =
            "Image size exceeds limit. Please use an image smaller than 10MB.";

    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_SOURCE_URL_LENGTH = 2048;

    private final HttpClient httpClient;
    private final UrlContentProperties properties;
    private final UrlDestinationValidator destinationValidator;

    public DefaultUrlContentService(
            HttpClient urlContentHttpClient,
            UrlContentProperties properties,
            UrlDestinationValidator destinationValidator) {
        this.httpClient = urlContentHttpClient;
        this.properties = properties;
        this.destinationValidator = destinationValidator;
    }

    @Override
    public DownloadedImage downloadImage(String imageUrl) {
        URI uri = parseAndValidate(imageUrl);
        FetchResponse response = fetch(uri);
        String contentType = response.contentType();
        if (!isSupportedImageType(contentType)) {
            closeQuietly(response.body());
            throw failure(SAFE_FORMAT_FAILURE_MESSAGE, null);
        }

        try (InputStream body = response.body()) {
            byte[] content = readLimited(body, properties.getMaxImageSize().toBytes());
            return new DownloadedImage(fileName(response.uri()), contentType, content);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof UrlContentException urlContentException) {
                throw urlContentException;
            }
            throw failure(SAFE_ACCESS_FAILURE_MESSAGE, exception);
        }
    }

    private FetchResponse fetch(URI initialUri) {
        URI current = initialUri;
        for (int redirect = 0; redirect <= properties.getMaxRedirects(); redirect++) {
            destinationValidator.validate(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(properties.getRequestTimeout())
                    .header("Accept", "image/jpeg,image/png,image/webp")
                    .header("User-Agent", "XhsCopywritingBot/1.0")
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    closeQuietly(response.body());
                    String location = response.headers().firstValue("Location")
                            .orElseThrow(() -> failure(SAFE_ACCESS_FAILURE_MESSAGE, null));
                    current = current.resolve(location);
                    continue;
                }
                if (status < 200 || status >= 300) {
                    closeQuietly(response.body());
                    throw failure(SAFE_ACCESS_FAILURE_MESSAGE, null);
                }
                long declaredLength = response.headers().firstValueAsLong("Content-Length")
                        .orElse(-1L);
                if (declaredLength > properties.getMaxImageSize().toBytes()) {
                    closeQuietly(response.body());
                    throw failure(SAFE_SIZE_FAILURE_MESSAGE, null);
                }
                String contentType = response.headers().firstValue("Content-Type")
                        .map(value -> value.split(";", 2)[0].trim())
                        .orElse(null);
                return new FetchResponse(current, response.body(), contentType);
            } catch (IOException exception) {
                throw failure(SAFE_ACCESS_FAILURE_MESSAGE, exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw failure(SAFE_ACCESS_FAILURE_MESSAGE, exception);
            } catch (IllegalArgumentException exception) {
                throw failure(SAFE_ACCESS_FAILURE_MESSAGE, exception);
            }
        }
        throw failure(SAFE_ACCESS_FAILURE_MESSAGE, null);
    }

    private URI parseAndValidate(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_SOURCE_URL_LENGTH) {
            throw failure(SAFE_ACCESS_FAILURE_MESSAGE, null);
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = Optional.ofNullable(uri.getScheme())
                    .map(item -> item.toLowerCase(Locale.ROOT))
                    .orElse("");
            if (!("http".equals(scheme) || "https".equals(scheme))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw failure(SAFE_ACCESS_FAILURE_MESSAGE, null);
            }
            destinationValidator.validate(uri);
            return uri;
        } catch (URISyntaxException exception) {
            throw failure(SAFE_ACCESS_FAILURE_MESSAGE, exception);
        }
    }

    private byte[] readLimited(InputStream inputStream, long maximumBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > maximumBytes) {
                throw failure(SAFE_SIZE_FAILURE_MESSAGE, null);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String fileName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "url-image";
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.isBlank() ? "url-image" : limit(name, 255);
    }

    private String limit(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }

    private boolean isSupportedImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/png", "image/webp" -> true;
            default -> false;
        };
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // The original URL failure is more useful than a close failure.
        }
    }

    private UrlContentException failure(String message, Throwable cause) {
        return cause == null
                ? new UrlContentException(message)
                : new UrlContentException(message, cause);
    }

    private record FetchResponse(URI uri, InputStream body, String contentType) {
    }
}
