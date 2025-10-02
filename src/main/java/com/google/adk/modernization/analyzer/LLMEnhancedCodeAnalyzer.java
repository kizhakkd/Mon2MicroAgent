package com.google.adk.modernization.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.adk.modernization.llm.LLMClient;
import com.google.adk.modernization.prompt.PromptManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class LLMEnhancedCodeAnalyzer implements CodeAnalyzer {
    private final JavaParser javaParser;
    private final LLMClient llmClient;
    private final PromptManager promptManager;
    private final List<PackageInfo> packages;
    private final List<ClassInfo> classes;
    private final List<DependencyInfo> dependencies;

    public LLMEnhancedCodeAnalyzer(LLMClient llmClient, PromptManager promptManager) {
        this.javaParser = new JavaParser();
        this.llmClient = llmClient;
        this.promptManager = promptManager;
        this.packages = new ArrayList<>();
        this.classes = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    @Override
    public CodeAnalysisResult analyze(Path sourcePath) {
        try {
            // Reset collections
            packages.clear();
            classes.clear();
            dependencies.clear();

            // Parse all Java files recursively
            File sourceDir = sourcePath.toFile();
            processDirectory(sourceDir);

            // Use LLM to enhance package analysis
            enhancePackageAnalysis();

            // Use LLM to identify semantic dependencies
            enhanceDependencyAnalysis();

            return new CodeAnalysisResult(packages, classes, dependencies);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze source code", e);
        }
    }

    private void processDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                processJavaFile(file);
            }
        }
    }

    private void processJavaFile(File file) {
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
            parseResult.getResult().ifPresent(cu -> 
                cu.accept(new ClassVisitor(), null)
            );
        } catch (Exception e) {
            System.err.println("Error processing file: " + file + " - " + e.getMessage());
        }
    }

    private void enhancePackageAnalysis() {
        // Prepare package structure for LLM analysis
        String packageStructure = packages.stream()
            .map(p -> p.name() + "\n  " + String.join("\n  ", p.subPackages()))
            .collect(Collectors.joining("\n"));

        // Get LLM insights about package organization
        String prompt = promptManager.getPrompt("analyze-package-structure", 
            Map.of("packages", packageStructure));

        try {
            String response = llmClient.complete(prompt, Map.of()).get();
            // Parse LLM response and update package information
            // This would typically involve parsing JSON response and updating package metadata
        } catch (Exception e) {
            System.err.println("Failed to enhance package analysis: " + e.getMessage());
        }
    }

    private void enhanceDependencyAnalysis() {
        // Group classes by package for context
        Map<String, List<ClassInfo>> packageClasses = classes.stream()
            .collect(Collectors.groupingBy(ClassInfo::packageName));

        // Analyze each package's classes for semantic dependencies
        for (Map.Entry<String, List<ClassInfo>> entry : packageClasses.entrySet()) {
            String packageName = entry.getKey();
            List<ClassInfo> packageClassList = entry.getValue();

            // Convert classes to string representation for LLM
            String classesString = packageClassList.stream()
                .map(c -> String.format("class %s {\n  %s\n}",
                    c.name(),
                    String.join("\n  ", c.methods())))
                .collect(Collectors.joining("\n\n"));

            // Get LLM insights about class relationships
            String prompt = promptManager.getPrompt("identify-aggregates",
                Map.of("classes", classesString));

            try {
                String response = llmClient.complete(prompt, Map.of()).get();
                // Parse LLM response and update dependency information
                // This would typically involve parsing JSON response and adding semantic dependencies
            } catch (Exception e) {
                System.err.println("Failed to enhance dependency analysis for package " + 
                    packageName + ": " + e.getMessage());
            }
        }
    }

    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(ClassOrInterfaceDeclaration clazz, Void arg) {
            super.visit(clazz, arg);

            // Extract class info
            String className = clazz.getNameAsString();
            String packageName = clazz.findCompilationUnit()
                .flatMap(cu -> cu.getPackageDeclaration())
                .map(pd -> pd.getNameAsString())
                .orElse("");

            List<String> methods = clazz.getMethods().stream()
                .map(m -> m.getNameAsString())
                .toList();

            List<String> fields = clazz.getFields().stream()
                .flatMap(f -> f.getVariables().stream())
                .map(v -> v.getNameAsString())
                .toList();

            List<String> annotations = clazz.getAnnotations().stream()
                .map(a -> a.getNameAsString())
                .toList();

            // Add class info
            classes.add(new ClassInfo(
                className,
                packageName,
                methods,
                fields,
                annotations
            ));

            // Extract package info if not already present
            if (!packageName.isEmpty() && 
                packages.stream().noneMatch(p -> p.name().equals(packageName))) {
                packages.add(new PackageInfo(
                    packageName,
                    packageName.replace('.', '/'),
                    List.of() // Sub-packages will be added during full directory scan
                ));
            }

            // Extract dependencies
            clazz.getExtendedTypes().forEach(t ->
                dependencies.add(new DependencyInfo(
                    className,
                    t.getNameAsString(),
                    DependencyType.INHERITANCE
                ))
            );

            // Add composition/aggregation dependencies from fields
            clazz.getFields().forEach(field -> {
                String fieldType = field.getElementType().asString();
                dependencies.add(new DependencyInfo(
                    className,
                    fieldType,
                    field.isFinal() ? DependencyType.COMPOSITION : DependencyType.AGGREGATION
                ));
            });
        }
    }
}