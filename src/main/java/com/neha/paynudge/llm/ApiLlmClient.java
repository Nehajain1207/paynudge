package com.neha.paynudge.llm;

import com.neha.paynudge.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calls a real model over an OpenAI-compatible HTTP endpoint (Gemini by default). */
public class ApiLlmClient implements LlmClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final int MAX_ATTEMPTS = 4;
    private final String baseUrl, apiKey, model;

    public ApiLlmClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl; this.apiKey = apiKey; this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("temperature", 0.2);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)))
                .build();
        try {
            HttpResponse<String> response = null;
            // The model API is sometimes briefly overloaded (429/5xx): retry with a growing pause.
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt == MAX_ATTEMPTS) break;
                Thread.sleep(1000L * attempt * attempt);
            }
            if (response.statusCode() != 200)
                throw new IllegalStateException("Model API returned " + response.statusCode() + ": " + response.body());
            Map<String, Object> parsed = Json.readMap(response.body());
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            return (Map<String, Object>) choices.get(0).get("message");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model call interrupted", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not reach the model API", e);
        }
    }
}