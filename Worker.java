package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Main {
    public static final List<String> links = List.of("https://examplelink.com");

    public static final String CONTENT_TYPE = "application/json";
    public static final String KEY_RESPONSE = "true";

    public static void main(String[] args) {
        checkAsync("1", System.out::println);
    }

    public static void checkAsync(String userId, Consumer<Boolean> callback) {
        List<CompletableFuture<Boolean>> futures = links.stream()
                .map(link -> {
                    CompletableFuture<Boolean> future = new CompletableFuture<>();
                    checkByExecutors(link, userId, future::complete);
                    return future;
                }).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().anyMatch(CompletableFuture::join))
                .thenAccept(callback);
    }

    public static void checkByExecutors(String url, String userId, Consumer<Boolean> callback) {
        ExecutorService executorService = Executors.newCachedThreadPool();

        CompletableFuture.supplyAsync(() -> {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString("{\"id\" : \"" + userId + "\"}"))
                    .uri(URI.create(url))
                    .header("Content-Type", CONTENT_TYPE)
                    .build();

            try {
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.body().contains(KEY_RESPONSE)) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }

            return false;
        }, executorService).thenAccept(callback);

        executorService.shutdown();
    }

}
