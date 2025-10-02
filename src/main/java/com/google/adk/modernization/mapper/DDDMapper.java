package com.google.adk.modernization.mapper;

import com.google.adk.modernization.analyzer.CodeAnalyzer.ClassInfo;
import java.util.List;
import java.util.Set;

public interface DDDMapper {
    /**
     * Maps classes to bounded contexts based on DDD principles.
     *
     * @param classes List of classes from the monolith
     * @return List of identified bounded contexts
     */
    List<BoundedContext> identifyBoundedContexts(List<ClassInfo> classes);

    /**
     * Represents a bounded context in DDD.
     */
    record BoundedContext(
        String name,
        String description,
        Set<String> aggregateRoots,
        Set<String> entities,
        Set<String> valueObjects,
        Set<String> repositories,
        Set<String> services,
        List<DomainEvent> domainEvents
    ) {}

    /**
     * Represents a domain event in DDD.
     */
    record DomainEvent(
        String name,
        String aggregateRoot,
        Set<String> payload
    ) {}

    /**
     * Maps a bounded context to a microservice candidate.
     */
    record MicroserviceCandidate(
        String name,
        BoundedContext boundedContext,
        Set<String> apis,
        Set<String> commands,
        Set<String> queries,
        List<String> requiredServices
    ) {}

    /**
     * Generate microservice candidates from bounded contexts.
     *
     * @param boundedContexts List of identified bounded contexts
     * @return List of microservice candidates
     */
    List<MicroserviceCandidate> generateMicroserviceCandidates(List<BoundedContext> boundedContexts);
}