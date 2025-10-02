package com.google.adk.modernization.planner.impl;

import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import com.google.adk.modernization.planner.StranglerPlanner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultStranglerPlanner implements StranglerPlanner {

    @Override
    public MigrationPlan generateMigrationPlan(List<MicroserviceCandidate> candidates) {
        // For each microservice candidate, create a migration phase
        List<MigrationPhase> phases = new ArrayList<>();
        
        for (int i = 0; i < candidates.size(); i++) {
            MicroserviceCandidate candidate = candidates.get(i);
            
            List<MigrationStep> steps = new ArrayList<>();
            steps.add(new MigrationStep(
                "Create microservice project structure",
                StepType.SETUP_INFRASTRUCTURE,
                candidate.name(),
                List.of(),
                List.of("Delete project structure")
            ));
            steps.add(new MigrationStep(
                "Move domain classes",
                StepType.DEPLOY_SERVICE,
                candidate.name(),
                List.of(),
                List.of("Revert domain class migration")
            ));
            steps.add(new MigrationStep(
                "Configure service endpoints",
                StepType.CONFIGURE_GATEWAY,
                candidate.name(),
                List.of(),
                List.of("Remove service endpoints")
            ));
            steps.add(new MigrationStep(
                "Migrate data",
                StepType.MIGRATE_DATA,
                candidate.name(),
                List.of(),
                List.of("Rollback data migration")
            ));
            steps.add(new MigrationStep(
                "Update client references",
                StepType.UPDATE_CLIENTS,
                candidate.name(),
                List.of(),
                List.of("Revert client updates")
            ));
            steps.add(new MigrationStep(
                "Validate migration",
                StepType.VALIDATE,
                candidate.name(),
                List.of(),
                List.of()
            ));

            ValidationStrategy validation = new ValidationStrategy(
                List.of("Integration", "Load", "Smoke"),
                List.of("ResponseTime", "ErrorRate", "Throughput"),
                95, // 95% success threshold
                List.of("ErrorRate > 5%", "ResponseTime > 2s")
            );

            phases.add(new MigrationPhase(
                i + 1,
                "Migrate " + candidate.name(),
                List.of(candidate),
                steps,
                validation
            ));
        }

        // Create gateway configuration
        var routes = candidates.stream()
            .map(c -> new RouteConfig(
                "/api/" + c.name().toLowerCase(),
                c.name() + "-service",
                100, // 100% of traffic
                true,
                List.of() // no special headers
            ))
            .toList();

        var gatewayConfig = new GatewayConfig(
            "Spring Cloud Gateway",
            routes,
            true,  // enable circuit breaker
            true   // enable rate limiting
        );

        // Create data migration strategy
        var dataMigrationStrategy = new DataMigrationStrategy(
            "CDC",
            List.of("Debezium", "Apache Kafka"),
            List.of(new DatabaseConfig(
                "MySQL",
                "8.0",
                List.of("public"),
                List.of("*")
            )),
            List.of(new DatabaseConfig(
                "PostgreSQL",
                "15",
                List.of("public"),
                List.of("*")
            )),
            true  // enable rollback
        );

        return new MigrationPlan(phases, gatewayConfig, dataMigrationStrategy);
    }
}