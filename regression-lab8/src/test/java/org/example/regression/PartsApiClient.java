package org.example.regression;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartsApiClient {
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"?([^\",}\\s]+)\"?");
    private final HttpClient httpClient;
    private final String baseUrl;

    public PartsApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public HttpResponse<String> getAllParts() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/parts"))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> createPart(String partNumber, String name, double price) throws Exception {
        String json = String.format(
                "{\"partNumber\":\"%s\",\"name\":\"%s\",\"manufacturer\":\"Bosch\",\"price\":%.2f,\"quantity\":10}",
                partNumber, name, price
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/parts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getPartById(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/parts/" + id))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void deletePartIfExists(String id) {
        if (id == null || id.isBlank()) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/parts/" + id))
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
        }
    }

    public static String extractId(String json) {
        if (json == null) return null;
        Matcher matcher = ID_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static <T> T awaitCondition(
            Supplier<T> supplier,
            Predicate<T> condition,
            Duration timeout,
            Duration pollInterval
    ) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        T value = supplier.get();
        while (!condition.test(value)) {
            if (Instant.now().isAfter(deadline)) {
                throw new TimeoutException("Condition not met within " + timeout.toMillis() + " ms");
            }
            Thread.sleep(pollInterval.toMillis());
            value = supplier.get();
        }
        return value;
    }
}