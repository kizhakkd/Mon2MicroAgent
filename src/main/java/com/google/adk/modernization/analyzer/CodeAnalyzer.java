package com.google.adk.modernization.analyzer;

import java.nio.file.Path;
import java.util.List;

public interface CodeAnalyzer {
    /**
     * Analyzes a Java monolith codebase and extracts its structure.
     *
     * @param sourcePath Path to the monolith source code
     * @return CodeAnalysisResult containing packages, classes, dependencies
     */
    CodeAnalysisResult analyze(Path sourcePath);

    /**
     * Result class containing the analysis of the monolith codebase.
     */
    record CodeAnalysisResult(
        List<PackageInfo> packages,
        List<ClassInfo> classes,
        List<DependencyInfo> dependencies
    ) {}

    /**
     * Information about a Java package.
     */
    record PackageInfo(
        String name,
        String path,
        List<String> subPackages
    ) {}

    /**
     * Information about a Java class.
     */
    record ClassInfo(
        String name,
        String packageName,
        List<String> methods,
        List<String> fields,
        List<String> annotations
    ) {}

    /**
     * Information about dependencies between classes.
     */
    record DependencyInfo(
        String sourceClass,
        String targetClass,
        DependencyType type
    ) {}

    /**
     * Types of dependencies between classes.
     */
    enum DependencyType {
        INHERITANCE,
        COMPOSITION,
        AGGREGATION,
        ASSOCIATION
    }
}