package com.google.adk.modernization.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class GooglePaLMClient implements LLMClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiEndpoint;
    private final String apiKey;

    public GooglePaLMClient(
            @Value("${llm.palm.endpoint}") String apiEndpoint,
            @Value("${llm.palm.key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiEndpoint = apiEndpoint;
        this.apiKey = apiKey;
    }

    @Override
    public CompletableFuture<String> complete(String prompt, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create request payload for Gemini API
                Map<String, Object> part = new HashMap<>();
                part.put("text", prompt);

                Map<String, Object> request = new HashMap<>();
                Map<String, Object> content = Map.of("parts", List.of(part));
                request.put("contents", List.of(content));

                // Add optional parameters if provided
                if (!parameters.isEmpty()) {
                    Map<String, Object> generationConfig = new HashMap<>();
                    if (parameters.containsKey("temperature")) {
                        generationConfig.put("temperature", parameters.get("temperature"));
                    }
                    if (parameters.containsKey("candidateCount") || parameters.containsKey("n")) {
                        generationConfig.put("candidateCount", parameters.getOrDefault("candidateCount", parameters.getOrDefault("n", 1)));
                    }
                    request.put("generationConfig", generationConfig);
                }

                // Build the URL safely using UriComponentsBuilder
                String url = UriComponentsBuilder
                    .fromHttpUrl(apiEndpoint)
                    .path("/v1beta/models/gemini-1.0-pro:generateContent")
                    .queryParam("key", apiKey)
                    .toUriString();

                // Log the URL (with redacted API key) and key prefix for debugging
                System.out.println("Making request to URL: " + url.replace(apiKey, "REDACTED_API_KEY"));
                System.out.println("Using API Key starting with: " + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "null"));

                // Call Gemini API
                String responseText = restTemplate.postForObject(
                    url,
                    request,
                    String.class
                );

                // Parse response to get generated text
                JsonNode responseJson = objectMapper.readTree(responseText);
                return responseJson
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            } catch (Exception e) {
                String errorDetail = e.getMessage();
                if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                    errorDetail = ((org.springframework.web.client.HttpClientErrorException) e).getResponseBodyAsString();
                }
                throw new RuntimeException("Failed to generate text: " + errorDetail, e);
            }
        });
    }

    @Override
    public <T> CompletableFuture<T> complete(String prompt, Class<T> responseType, Map<String, Object> parameters) {
        return complete(prompt, parameters)
            .thenApply(response -> {
                try {
                    return objectMapper.readValue(response, responseType);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse LLM response", e);
                }
            });
    }
}