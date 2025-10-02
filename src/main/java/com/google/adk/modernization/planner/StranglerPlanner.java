package com.google.adk.modernization.planner;

import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import java.util.List;

public interface StranglerPlanner {
    /**
     * Generates a migration plan for strangling the monolith.
     *
     * @param candidates List of microservice candidates to migrate to
     * @return MigrationPlan containing phases and steps
     */
    MigrationPlan generateMigrationPlan(List<MicroserviceCandidate> candidates);

    /**
     * Represents a complete migration plan.
     */
    record MigrationPlan(
        List<MigrationPhase> phases,
        GatewayConfig gatewayConfig,
        DataMigrationStrategy dataMigrationStrategy
    ) {}

    /**
     * Represents a phase in the migration plan.
     */
    record MigrationPhase(
        int phaseNumber,
        String description,
        List<MicroserviceCandidate> services,
        List<MigrationStep> steps,
        ValidationStrategy validation
    ) {}

    /**
     * Represents a specific step in the migration.
     */
    record MigrationStep(
        String description,
        StepType type,
        String service,
        List<String> dependencies,
        List<String> rollbackSteps
    ) {}

    /**
     * Types of migration steps.
     */
    enum StepType {
        SETUP_INFRASTRUCTURE,
        DEPLOY_SERVICE,
        CONFIGURE_GATEWAY,
        MIGRATE_DATA,
        UPDATE_CLIENTS,
        VALIDATE,
        ROLLBACK
    }

    /**
     * Gateway configuration for traffic routing.
     */
    record GatewayConfig(
        String type, // e.g., "Spring Cloud Gateway", "Envoy"
        List<RouteConfig> routes,
        boolean enableCircuitBreaker,
        boolean enableRateLimiting
    ) {}

    /**
     * Route configuration for the gateway.
     */
    record RouteConfig(
        String path,
        String destinationService,
        int weight, // percentage of traffic (0-100)
        boolean stripPrefix,
        List<String> headers
    ) {}

    /**
     * Strategy for data migration.
     */
    record DataMigrationStrategy(
        String type, // e.g., "CDC", "Dual Write"
        List<String> tools, // e.g., "Debezium", "Apache Kafka"
        List<DatabaseConfig> sourceConfigs,
        List<DatabaseConfig> targetConfigs,
        boolean enableRollback
    ) {}

    /**
     * Database configuration for migration.
     */
    record DatabaseConfig(
        String type, // e.g., "MySQL", "PostgreSQL"
        String version,
        List<String> schemas,
        List<String> tables
    ) {}

    /**
     * Strategy for validating the migration.
     */
    record ValidationStrategy(
        List<String> testTypes, // e.g., "Integration", "Load", "Smoke"
        List<String> metrics, // e.g., "ResponseTime", "ErrorRate"
        int successThreshold, // percentage
        List<String> rollbackTriggers
    ) {}
}