package com.google.adk.modernization.refactor;

import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface RefactorEngine {
    /**
     * Refactor and move code from monolith to microservice.
     *
     * @param monolithPath Path to the monolith source code
     * @param candidate Microservice candidate to migrate code to
     * @param targetPath Path to the target microservice project
     * @return List of refactored files
     */
    List<Path> refactorCode(Path monolithPath, MicroserviceCandidate candidate, Path targetPath);

    /**
     * Configuration for code refactoring.
     */
    record RefactorConfig(
        boolean preserveOriginal,
        boolean addComments,
        boolean updateReferences,
        List<String> excludePatterns
    ) {}

    /**
     * Result of a refactoring operation.
     */
    record RefactorResult(
        List<Path> refactoredFiles,
        List<String> warnings,
        Map<String, String> oldToNewPaths
    ) {}

    /**
     * Definition of code to be refactored.
     */
    record RefactorDefinition(
        String sourcePath,
        String targetPath,
        List<String> dependencies,
        List<CodeTransformation> transformations
    ) {}

    /**
     * Defines a code transformation.
     */
    record CodeTransformation(
        TransformationType type,
        String pattern,
        String replacement,
        List<String> conditions
    ) {}

    /**
     * Types of code transformations.
     */
    enum TransformationType {
        MOVE,           // Move code to new location
        RENAME,         // Rename class/method/variable
        EXTRACT,        // Extract method/class
        UPDATE_IMPORTS, // Update import statements
        ADD_ANNOTATION, // Add new annotation
        MODIFY_ACCESS   // Modify access level
    }
}