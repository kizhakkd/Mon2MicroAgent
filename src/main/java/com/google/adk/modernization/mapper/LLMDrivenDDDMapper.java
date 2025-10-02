package com.google.adk.modernization.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.modernization.analyzer.CodeAnalyzer.ClassInfo;
import com.google.adk.modernization.llm.LLMClient;
import com.google.adk.modernization.prompt.PromptManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class LLMDrivenDDDMapper implements DDDMapper {
    private final LLMClient llmClient;
    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    public LLMDrivenDDDMapper(
            LLMClient llmClient,
            PromptManager promptManager,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.promptManager = promptManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<BoundedContext> identifyBoundedContexts(List<ClassInfo> classes) {
        try {
            // Prepare class information for LLM analysis
            String classesJson = objectMapper.writeValueAsString(classes);
            
            // Get LLM analysis for bounded contexts
            String prompt = promptManager.getPrompt("identify-bounded-contexts",
                Map.of("classes", classesJson));
            
            String response = llmClient.complete(prompt, Map.of()).get();
            
            // Parse LLM response into bounded contexts
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> boundedContextsMap = (List<Map<String, Object>>) result.get("boundedContexts");
            
            return boundedContextsMap.stream()
                .map(this::convertToBoundedContext)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to identify bounded contexts", e);
        }
    }

    @Override
    public List<MicroserviceCandidate> generateMicroserviceCandidates(List<BoundedContext> boundedContexts) {
        List<CompletableFuture<MicroserviceCandidate>> futures = boundedContexts.stream()
            .map(context -> {
                try {
                    String contextJson = objectMapper.writeValueAsString(context);
                    String prompt = promptManager.getPrompt("microservice-candidate-design",
                        Map.of("boundedContext", contextJson));
                    
                    return llmClient.complete(prompt, Map.of())
                        .thenApply(response -> {
                            try {
                                Map<String, Object> result = objectMapper.readValue(response, Map.class);
                                Map<String, Object> microservice = (Map<String, Object>) result.get("microservice");
                                return convertToMicroserviceCandidate(microservice, context);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to parse microservice candidate", e);
                            }
                        });
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate microservice candidate", e);
                }
            })
            .collect(Collectors.toList());

        // Wait for all candidates to be generated
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    private BoundedContext convertToBoundedContext(Map<String, Object> map) {
        return new BoundedContext(
            (String) map.get("name"),
            (String) map.get("description"),
            new HashSet<>((List<String>) map.get("aggregateRoots")),
            new HashSet<>((List<String>) map.get("entities")),
            new HashSet<>((List<String>) map.get("valueObjects")),
            new HashSet<>((List<String>) map.get("repositories")),
            new HashSet<>((List<String>) map.get("services")),
            ((List<Map<String, Object>>) map.get("relationships")).stream()
                .map(rel -> new DomainEvent(
                    (String) rel.get("name"),
                    (String) rel.get("aggregateRoot"),
                    new HashSet<>((List<String>) rel.get("payload"))
                ))
                .collect(Collectors.toList())
        );
    }

    private MicroserviceCandidate convertToMicroserviceCandidate(
            Map<String, Object> map,
            BoundedContext context) {
        
        List<Map<String, Object>> apis = (List<Map<String, Object>>) map.get("apis");
        Set<String> apiPaths = apis.stream()
            .map(api -> (String) api.get("path"))
            .collect(Collectors.toSet());

        List<Map<String, Object>> events = (List<Map<String, Object>>) map.get("events");
        Set<String> commands = events.stream()
            .filter(e -> ((String) e.get("type")).equals("COMMAND"))
            .map(e -> (String) e.get("name"))
            .collect(Collectors.toSet());

        Set<String> queries = events.stream()
            .filter(e -> ((String) e.get("type")).equals("QUERY"))
            .map(e -> (String) e.get("name"))
            .collect(Collectors.toSet());

        return new MicroserviceCandidate(
            (String) map.get("name"),
            context,
            apiPaths,
            commands,
            queries,
            (List<String>) map.get("dependencies")
        );
    }
}