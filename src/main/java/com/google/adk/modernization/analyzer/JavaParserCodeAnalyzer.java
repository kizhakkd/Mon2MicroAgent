package com.google.adk.modernization.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class JavaParserCodeAnalyzer implements CodeAnalyzer {
    private final JavaParser javaParser;
    private final List<PackageInfo> packages;
    private final List<ClassInfo> classes;
    private final List<DependencyInfo> dependencies;

    public JavaParserCodeAnalyzer() {
        this.javaParser = new JavaParser();
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
            // Log error but continue processing other files
            System.err.println("Error processing file: " + file + " - " + e.getMessage());
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