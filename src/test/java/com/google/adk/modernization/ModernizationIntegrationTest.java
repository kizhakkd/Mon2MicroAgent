package com.google.adk.modernization;

import com.google.adk.modernization.analyzer.CodeAnalyzer;
import com.google.adk.modernization.analyzer.CodeAnalyzer.CodeAnalysisResult;
import com.google.adk.modernization.analyzer.JavaParserCodeAnalyzer;
import com.google.adk.modernization.analyzer.LLMEnhancedCodeAnalyzer;
import com.google.adk.modernization.mapper.DDDMapper;
import com.google.adk.modernization.mapper.DDDMapper.BoundedContext;
import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import com.google.adk.modernization.planner.StranglerPlanner;
import com.google.adk.modernization.planner.StranglerPlanner.MigrationPlan;
import com.google.adk.modernization.generator.ProjectGenerator;
import com.google.adk.modernization.refactor.RefactorEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "llm.palm.key=test-key",
    "agent.workspace.monolith=./test-monolith",
    "agent.workspace.output=./output"
})
class ModernizationIntegrationTest {

    @Autowired
    private LLMEnhancedCodeAnalyzer codeAnalyzer;

    @Autowired
    private DDDMapper dddMapper;

    @Autowired
    private StranglerPlanner planner;

    @Autowired
    private ProjectGenerator generator;

    @Autowired
    private RefactorEngine refactorEngine;

    @Test
    void testFullModernizationProcess() throws Exception {
        // Step 1: Analyze monolith
        Path monolithPath = Paths.get("test-monolith");
        CodeAnalysisResult analysis = codeAnalyzer.analyze(monolithPath);

        // Verify analysis results
        assertNotNull(analysis);
        assertFalse(analysis.classes().isEmpty(), "Should find Java classes");
        assertTrue(analysis.classes().stream()
            .anyMatch(c -> c.name().contains("Pet") || c.name().contains("Vet")),
            "Should find domain classes");

        // Step 2: Identify bounded contexts
        List<BoundedContext> contexts = dddMapper.identifyBoundedContexts(analysis.classes());

        // Verify bounded contexts
        assertNotNull(contexts);
        assertFalse(contexts.isEmpty(), "Should identify bounded contexts");
        assertTrue(contexts.stream()
            .anyMatch(bc -> bc.name().contains("Clinic") || bc.name().contains("Pet")),
            "Should identify main domain contexts");

        // Step 3: Generate microservice candidates
        List<MicroserviceCandidate> candidates = dddMapper.generateMicroserviceCandidates(contexts);

        // Verify candidates
        assertNotNull(candidates);
        assertFalse(candidates.isEmpty(), "Should generate microservice candidates");
        assertTrue(candidates.stream()
            .anyMatch(c -> c.name().contains("pet") || c.name().contains("vet")),
            "Should identify service candidates");

        // Step 4: Create migration plan
        MigrationPlan plan = planner.generateMigrationPlan(candidates);

        // Verify plan
        assertNotNull(plan);
        assertFalse(plan.phases().isEmpty(), "Should create migration phases");
        assertTrue(plan.phases().get(0).services().size() > 0,
            "Should include services in first phase");

        // Step 5: Generate and refactor services
        Path outputPath = Paths.get("output");
        for (MicroserviceCandidate candidate : candidates) {
            // Generate service project
            Path servicePath = generator.generateMicroservice(candidate, outputPath);
            assertNotNull(servicePath, "Should generate service project");

            // Refactor code
            List<Path> refactoredFiles = refactorEngine.refactorCode(monolithPath, candidate, servicePath);
            assertFalse(refactoredFiles.isEmpty(), "Should refactor files");
        }
    }
}