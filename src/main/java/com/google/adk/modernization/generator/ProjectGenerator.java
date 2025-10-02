package com.google.adk.modernization.generator;

import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import java.nio.file.Path;

public interface ProjectGenerator {
    /**
     * Generates a new Spring Boot microservice project with DDD structure.
     *
     * @param candidate The microservice candidate to generate
     * @param outputPath The path where the project should be generated
     * @return The path to the generated project
     */
    Path generateMicroservice(MicroserviceCandidate candidate, Path outputPath);

    /**
     * Configuration for a generated microservice.
     */
    record ServiceConfig(
        String groupId,
        String artifactId,
        String version,
        String description,
        String packageName,
        String springBootVersion,
        boolean useKubernetes,
        boolean useHelm,
        boolean useGithubActions
    ) {}

    /**
     * Represents the structure of the generated project.
     */
    record ProjectStructure(
        Path rootDir,
        Path srcMainJava,
        Path srcMainResources,
        Path srcTestJava,
        Path srcTestResources,
        Path dockerDir,
        Path kubernetesDir,
        Path helmDir,
        Path githubDir
    ) {}
}