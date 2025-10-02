package com.google.adk.modernization.llm;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for interacting with Large Language Models.
 */
public interface LLMClient {
    /**
     * Send a prompt to the LLM and get a completion.
     *
     * @param prompt The prompt text
     * @param parameters Additional parameters for the LLM
     * @return The LLM's response
     */
    CompletableFuture<String> complete(String prompt, Map<String, Object> parameters);

    /**
     * Send a structured prompt to the LLM and get a typed response.
     *
     * @param prompt The prompt text
     * @param responseType The expected response type
     * @param parameters Additional parameters for the LLM
     * @return The LLM's response converted to the specified type
     */
    <T> CompletableFuture<T> complete(String prompt, Class<T> responseType, Map<String, Object> parameters);
}