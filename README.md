# Mon2MicroAgent: AI-Powered Monolith to Microservices Migration Agent

An autonomous agent powered by Large Language Models (LLMs) for analyzing and migrating monolithic Java applications to domain-driven microservices using the Strangler Fig pattern.

## Overview

Mon2MicroAgent is an intelligent toolkit that uses LLMs to assist in every step of the modernization process, from analyzing monolithic codebases to generating and refactoring microservices. It combines the power of static code analysis with AI-driven insights to make informed decisions about service boundaries, migration strategies, and implementation details.

## Key Features

- **AI-Powered Analysis**: Uses LLMs to analyze code structure, identify domain boundaries, and suggest service decomposition
- **Domain-Driven Design**: Automatically identifies bounded contexts, aggregates, and domain events
- **Intelligent Migration Planning**: Creates optimal migration strategies based on dependencies and business impact
- **Automated Code Generation**: Scaffolds new microservices with proper DDD structure and infrastructure code
- **Smart Code Refactoring**: Uses AI to refactor and adapt code for the microservice architecture
- **Configurable Prompts**: Externalized prompt templates for customizing the AI's behavior

## Architecture

### Core Components

1. **CodeAnalyzer**
   - Static analysis of Java code using JavaParser
   - LLM-enhanced understanding of code structure and relationships
   - Intelligent package and dependency analysis

2. **DDDMapper**
   - AI-driven identification of bounded contexts
   - Smart mapping of domain models to DDD concepts
   - Context mapping and relationship analysis

3. **StranglerPlanner**
   - LLM-powered migration strategy generation
   - Intelligent service dependency analysis
   - Risk-aware phasing and validation planning

4. **ProjectGenerator**
   - Template-based microservice scaffolding
   - Infrastructure-as-Code generation
   - CI/CD pipeline configuration

5. **RefactorEngine**
   - AI-assisted code migration
   - Smart refactoring pattern application
   - Dependency updates and reference management

### LLM Integration

The agent uses Google's PaLM API for:
- Code analysis and understanding
- Domain boundary identification
- Migration strategy planning
- Refactoring decisions

### Prompt Management

Prompts are externalized in YAML files for:
- Easy customization and versioning
- Domain-specific terminology adaptation
- Migration strategy tuning
- Company-specific preferences

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.8+
- Google PaLM API key

### Configuration

1. Configure LLM settings in `application.yml`:
   ```yaml
   llm:
     palm:
       endpoint: https://generativelanguage.googleapis.com/v1beta3/models/text-bison-001:generateText
       key: your-api-key
   ```

2. Customize prompts in `src/main/resources/prompts/`:
   - `domain-analysis.yaml`: Domain and bounded context analysis
   - `migration-planning.yaml`: Migration strategy and planning
   - Add custom prompt files as needed

### Usage

1. **Analyze Monolith**
   ```java
   CodeAnalyzer analyzer = context.getBean(CodeAnalyzer.class);
   CodeAnalysisResult result = analyzer.analyze(monolithPath);
   ```

2. **Identify Domains**
   ```java
   DDDMapper mapper = context.getBean(DDDMapper.class);
   List<BoundedContext> contexts = mapper.identifyBoundedContexts(result.classes());
   ```

3. **Plan Migration**
   ```java
   StranglerPlanner planner = context.getBean(StranglerPlanner.class);
   MigrationPlan plan = planner.generateMigrationPlan(mapper.generateMicroserviceCandidates(contexts));
   ```

4. **Generate Services**
   ```java
   ProjectGenerator generator = context.getBean(ProjectGenerator.class);
   for (MicroserviceCandidate candidate : candidates) {
       Path servicePath = generator.generateMicroservice(candidate, outputPath);
   }
   ```

5. **Refactor Code**
   ```java
   RefactorEngine refactor = context.getBean(RefactorEngine.class);
   List<Path> refactoredFiles = refactor.refactorCode(monolithPath, candidate, servicePath);
   ```

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.