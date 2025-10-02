package com.google.adk.modernization.generator;

import com.google.adk.modernization.mapper.DDDMapper.MicroserviceCandidate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class SpringBootProjectGenerator implements ProjectGenerator {
    private static final String TEMPLATE_DIR = "/templates/spring-boot";

    @Override
    public Path generateMicroservice(MicroserviceCandidate candidate, Path outputPath) {
        try {
            // Create service configuration
            ServiceConfig config = createServiceConfig(candidate);

            // Create project structure
            ProjectStructure structure = createProjectStructure(outputPath, config);

            // Generate project files
            generateProjectFiles(structure, config, candidate);

            return structure.rootDir();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate microservice project", e);
        }
    }

    private ServiceConfig createServiceConfig(MicroserviceCandidate candidate) {
        return new ServiceConfig(
            "com.example", // Should be configurable
            candidate.name(),
            "0.1.0-SNAPSHOT",
            "Microservice for " + candidate.boundedContext().name() + " bounded context",
            "com.example." + candidate.name().replace("-", ""),
            "3.1.4",
            true,  // Use Kubernetes
            true,  // Use Helm
            true   // Use GitHub Actions
        );
    }

    private ProjectStructure createProjectStructure(Path outputPath, ServiceConfig config) throws IOException {
        Path rootDir = outputPath.resolve(config.artifactId());
        
        // Create main directories
        Path srcMainJava = createDirectories(rootDir, "src/main/java");
        Path srcMainResources = createDirectories(rootDir, "src/main/resources");
        Path srcTestJava = createDirectories(rootDir, "src/test/java");
        Path srcTestResources = createDirectories(rootDir, "src/test/resources");

        // Create infrastructure directories
        Path dockerDir = createDirectories(rootDir, "docker");
        Path kubernetesDir = createDirectories(rootDir, "kubernetes");
        Path helmDir = createDirectories(rootDir, "helm", config.artifactId());
        Path githubDir = createDirectories(rootDir, ".github/workflows");

        // Create DDD layer directories
        String basePackagePath = config.packageName().replace(".", "/");
        createDirectories(srcMainJava, basePackagePath, "domain/model");
        createDirectories(srcMainJava, basePackagePath, "domain/repository");
        createDirectories(srcMainJava, basePackagePath, "domain/service");
        createDirectories(srcMainJava, basePackagePath, "application/service");
        createDirectories(srcMainJava, basePackagePath, "application/dto");
        createDirectories(srcMainJava, basePackagePath, "infrastructure/persistence");
        createDirectories(srcMainJava, basePackagePath, "infrastructure/messaging");
        createDirectories(srcMainJava, basePackagePath, "interfaces/rest");

        return new ProjectStructure(
            rootDir, srcMainJava, srcMainResources, 
            srcTestJava, srcTestResources, dockerDir,
            kubernetesDir, helmDir, githubDir
        );
    }

    private void generateProjectFiles(
            ProjectStructure structure,
            ServiceConfig config,
            MicroserviceCandidate candidate) throws IOException {
        
        // Generate build files
        generatePomXml(structure.rootDir(), config, candidate);
        
        // Generate application properties
        generateApplicationYaml(structure.srcMainResources());
        
        // Generate Docker files
        generateDockerfile(structure.dockerDir(), config);
        
        // Generate Kubernetes manifests
        if (config.useKubernetes()) {
            generateKubernetesManifests(structure.kubernetesDir(), config);
        }
        
        // Generate Helm charts
        if (config.useHelm()) {
            generateHelmCharts(structure.helmDir(), config);
        }
        
        // Generate GitHub Actions
        if (config.useGithubActions()) {
            generateGithubActions(structure.githubDir(), config);
        }
        
        // Generate domain model classes
        generateDomainModel(structure, config, candidate);
        
        // Generate Spring Boot application class
        generateApplicationClass(structure, config);
    }

    private Path createDirectories(Path base, String... paths) throws IOException {
        Path current = base;
        for (String path : paths) {
            current = current.resolve(path);
            Files.createDirectories(current);
        }
        return current;
    }

    private void generatePomXml(Path rootDir, ServiceConfig config, MicroserviceCandidate candidate) throws IOException {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>%s</version>
                </parent>
                
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <description>%s</description>
                
                <properties>
                    <java.version>17</java.version>
                    <spring-cloud.version>2023.0.0</spring-cloud.version>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-jpa</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.kafka</groupId>
                        <artifactId>spring-kafka</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <optional>true</optional>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-test</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-dependencies</artifactId>
                            <version>${spring-cloud.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                            <configuration>
                                <excludes>
                                    <exclude>
                                        <groupId>org.projectlombok</groupId>
                                        <artifactId>lombok</artifactId>
                                    </exclude>
                                </excludes>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """.formatted(
                config.springBootVersion(),
                config.groupId(),
                config.artifactId(),
                config.version(),
                config.description()
            );
        
        Files.writeString(rootDir.resolve("pom.xml"), pomXml);
    }

    private void generateApplicationYaml(Path resourcesDir) throws IOException {
        String yaml = """
            spring:
              application:
                name: ${spring.application.name}
              datasource:
                url: jdbc:postgresql://localhost:5432/${spring.application.name}
                username: postgres
                password: postgres
              jpa:
                hibernate:
                  ddl-auto: update
                properties:
                  hibernate:
                    dialect: org.hibernate.dialect.PostgreSQLDialect
              kafka:
                bootstrap-servers: localhost:9092
                
            server:
              port: 8080
              
            eureka:
              client:
                serviceUrl:
                  defaultZone: http://localhost:8761/eureka/
                  
            management:
              endpoints:
                web:
                  exposure:
                    include: health,info,metrics
            """;
        
        Files.writeString(resourcesDir.resolve("application.yml"), yaml);
    }

    private void generateDockerfile(Path dockerDir, ServiceConfig config) throws IOException {
        String dockerfile = """
            FROM eclipse-temurin:17-jre-alpine
            WORKDIR /app
            COPY target/*.jar app.jar
            EXPOSE 8080
            ENTRYPOINT ["java", "-jar", "app.jar"]
            """;
        
        Files.writeString(dockerDir.resolve("Dockerfile"), dockerfile);
    }

    private void generateKubernetesManifests(Path kubernetesDir, ServiceConfig config) throws IOException {
        String deployment = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: %s
            spec:
              replicas: 3
              selector:
                matchLabels:
                  app: %s
              template:
                metadata:
                  labels:
                    app: %s
                spec:
                  containers:
                  - name: %s
                    image: %s:%s
                    ports:
                    - containerPort: 8080
                    livenessProbe:
                      httpGet:
                        path: /actuator/health
                        port: 8080
                    readinessProbe:
                      httpGet:
                        path: /actuator/health
                        port: 8080
            """.formatted(
                config.artifactId(),
                config.artifactId(),
                config.artifactId(),
                config.artifactId(),
                config.artifactId(),
                config.version()
            );
        
        Files.writeString(kubernetesDir.resolve("deployment.yaml"), deployment);

        String service = """
            apiVersion: v1
            kind: Service
            metadata:
              name: %s
            spec:
              selector:
                app: %s
              ports:
              - port: 80
                targetPort: 8080
              type: ClusterIP
            """.formatted(
                config.artifactId(),
                config.artifactId()
            );
        
        Files.writeString(kubernetesDir.resolve("service.yaml"), service);
    }

    private void generateHelmCharts(Path helmDir, ServiceConfig config) throws IOException {
        // Generate Chart.yaml
        String chart = """
            apiVersion: v2
            name: %s
            description: %s
            version: %s
            type: application
            """.formatted(
                config.artifactId(),
                config.description(),
                config.version()
            );
        
        Files.writeString(helmDir.resolve("Chart.yaml"), chart);

        // Generate values.yaml
        String values = """
            replicaCount: 3
            
            image:
              repository: %s
              tag: %s
              pullPolicy: IfNotPresent
            
            service:
              type: ClusterIP
              port: 80
            
            ingress:
              enabled: false
            
            resources:
              limits:
                cpu: 500m
                memory: 512Mi
              requests:
                cpu: 250m
                memory: 256Mi
            """.formatted(
                config.artifactId(),
                config.version()
            );
        
        Files.writeString(helmDir.resolve("values.yaml"), values);
    }

    private void generateGithubActions(Path githubDir, ServiceConfig config) throws IOException {
        String workflow = """
            name: CI/CD
            
            on:
              push:
                branches: [ main ]
              pull_request:
                branches: [ main ]
            
            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                - uses: actions/checkout@v4
                
                - name: Set up JDK 17
                  uses: actions/setup-java@v3
                  with:
                    java-version: '17'
                    distribution: 'temurin'
                
                - name: Build with Maven
                  run: mvn clean package
                
                - name: Build Docker image
                  run: |
                    docker build -t %s:%s .
                    
                - name: Run tests
                  run: mvn test
            """.formatted(
                config.artifactId(),
                config.version()
            );
        
        Files.writeString(githubDir.resolve("ci-cd.yml"), workflow);
    }

    private void generateDomainModel(ProjectStructure structure, ServiceConfig config, MicroserviceCandidate candidate) throws IOException {
        String basePackagePath = config.packageName().replace(".", "/");
        Path domainModelDir = structure.srcMainJava().resolve(basePackagePath).resolve("domain/model");

        // Generate aggregate roots
        for (String aggregate : candidate.boundedContext().aggregateRoots()) {
            String aggregateClass = """
                package %s.domain.model;
                
                import lombok.Data;
                import javax.persistence.Entity;
                import javax.persistence.Id;
                import javax.persistence.Version;
                import java.util.UUID;
                
                @Data
                @Entity
                public class %s {
                    @Id
                    private UUID id;
                    
                    @Version
                    private Long version;
                    
                    // Add fields based on domain model
                }
                """.formatted(
                    config.packageName(),
                    aggregate
                );
            
            Files.writeString(domainModelDir.resolve(aggregate + ".java"), aggregateClass);
        }
    }

    private void generateApplicationClass(ProjectStructure structure, ServiceConfig config) throws IOException {
        String applicationClass = """
            package %s;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
            
            @SpringBootApplication
            @EnableDiscoveryClient
            public class Application {
                public static void main(String[] args) {
                    SpringApplication.run(Application.class, args);
                }
            }
            """.formatted(config.packageName());
        
        String basePackagePath = config.packageName().replace(".", "/");
        Files.writeString(
            structure.srcMainJava().resolve(basePackagePath).resolve("Application.java"),
            applicationClass
        );
    }
}