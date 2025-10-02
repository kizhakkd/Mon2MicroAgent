package com.google.adk.modernization.refactor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.modernization.llm.LLMClient;
import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import com.google.adk.modernization.prompt.PromptManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class LLMDrivenRefactorEngine implements RefactorEngine {
    private final LLMClient llmClient;
    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    public LLMDrivenRefactorEngine(
            LLMClient llmClient,
            PromptManager promptManager,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.promptManager = promptManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Path> refactorCode(
            Path monolithPath,
            MicroserviceCandidate candidate,
            Path targetPath) {
        try {
            List<Path> refactoredFiles = new ArrayList<>();
            Map<String, String> fileCache = new HashMap<>();

            // Find all Java files that need to be refactored
            List<Path> sourceFiles = findSourceFiles(monolithPath, candidate);

            // Process each file
            for (Path sourceFile : sourceFiles) {
                // Read source file
                String sourceCode = Files.readString(sourceFile);
                String targetContext = objectMapper.writeValueAsString(candidate);

                // Get refactoring plan from LLM
                String refactorPlan = getRefactoringPlan(sourceCode, targetContext);

                // Execute refactoring steps
                RefactorResult result = executeRefactoring(sourceFile, refactorPlan, targetPath);
                refactoredFiles.addAll(result.refactoredFiles());

                // Cache file content for dependency updates
                fileCache.put(sourceFile.toString(), Files.readString(result.refactoredFiles().get(0)));
            }

            // Update dependencies in refactored files
            updateDependencies(refactoredFiles, fileCache, candidate);

            return refactoredFiles;
        } catch (Exception e) {
            throw new RuntimeException("Failed to refactor code", e);
        }
    }

    private List<Path> findSourceFiles(Path monolithPath, MicroserviceCandidate candidate) throws IOException {
        // Find all Java files that belong to this bounded context
        Set<String> contextClasses = new HashSet<>();
        contextClasses.addAll(candidate.boundedContext().aggregateRoots());
        contextClasses.addAll(candidate.boundedContext().entities());
        contextClasses.addAll(candidate.boundedContext().valueObjects());
        contextClasses.addAll(candidate.boundedContext().repositories());
        contextClasses.addAll(candidate.boundedContext().services());

        return Files.walk(monolithPath)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .filter(p -> contextClasses.stream()
                .anyMatch(cls -> p.getFileName().toString().equals(cls + ".java")))
            .collect(Collectors.toList());
    }

    private String getRefactoringPlan(String sourceCode, String targetContext) throws Exception {
        String prompt = promptManager.getPrompt("refactor-class",
            Map.of(
                "sourceCode", sourceCode,
                "targetContext", targetContext
            ));

        return llmClient.complete(prompt, Map.of()).get();
    }

    private RefactorResult executeRefactoring(Path sourceFile, String refactorPlan, Path targetPath) throws Exception {
        Map<String, Object> plan = objectMapper.readValue(refactorPlan, Map.class);
        Map<String, Object> refactoring = (Map<String, Object>) plan.get("refactoring");
        
        // Create target directory if needed
        String newLocation = (String) refactoring.get("newLocation");
        Path targetFile = targetPath.resolve(newLocation);
        Files.createDirectories(targetFile.getParent());

        // Apply refactoring steps
        String currentContent = Files.readString(sourceFile);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) refactoring.get("steps");
        
        for (Map<String, Object> step : steps) {
            String oldCode = (String) step.get("oldCode");
            String newCode = (String) step.get("newCode");
            currentContent = currentContent.replace(oldCode, newCode);
        }

        // Write refactored file
        Files.writeString(targetFile, currentContent);

        return new RefactorResult(
            List.of(targetFile),
            new ArrayList<>(),
            Map.of(sourceFile.toString(), targetFile.toString())
        );
    }

    private void updateDependencies(
            List<Path> refactoredFiles,
            Map<String, String> fileCache,
            MicroserviceCandidate candidate) throws Exception {
        
        for (Path file : refactoredFiles) {
            String content = fileCache.get(file.toString());
            if (content == null) continue;

            String prompt = promptManager.getPrompt("dependency-update",
                Map.of(
                    "originalDependencies", extractImports(content),
                    "refactoredClass", content,
                    "serviceContext", objectMapper.writeValueAsString(candidate)
                ));

            String response = llmClient.complete(prompt, Map.of()).get();
            Map<String, Object> updates = objectMapper.readValue(response, Map.class);

            // Apply dependency updates
            List<Map<String, Object>> changes = (List<Map<String, Object>>) updates.get("updates");
            String updatedContent = content;
            
            for (Map<String, Object> change : changes) {
                List<String> codeChanges = (List<String>) change.get("codeChanges");
                for (String changeStr : codeChanges) {
                    String[] parts = changeStr.split("->>");
                    if (parts.length == 2) {
                        updatedContent = updatedContent.replace(parts[0].trim(), parts[1].trim());
                    }
                }
            }

            Files.writeString(file, updatedContent);
        }
    }

    private String extractImports(String content) {
        return Arrays.stream(content.split("\n"))
            .filter(line -> line.trim().startsWith("import "))
            .collect(Collectors.joining("\n"));
    }
}