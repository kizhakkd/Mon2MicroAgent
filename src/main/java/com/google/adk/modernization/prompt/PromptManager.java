package com.google.adk.modernization.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PromptManager {
    private final Map<String, PromptTemplate> prompts;
    private final ObjectMapper yamlMapper;

    public PromptManager() {
        this.prompts = new HashMap<>();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        loadPrompts();
    }

    private void loadPrompts() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:prompts/**/*.yaml");

            for (Resource resource : resources) {
                PromptConfig config = yamlMapper.readValue(
                    resource.getInputStream(),
                    PromptConfig.class
                );

                for (PromptTemplate template : config.templates()) {
                    prompts.put(template.id(), template);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompts", e);
        }
    }

    public String getPrompt(String id, Map<String, Object> variables) {
        PromptTemplate template = prompts.get(id);
        if (template == null) {
            throw new IllegalArgumentException("Unknown prompt ID: " + id);
        }

        return template.format(variables);
    }

    record PromptConfig(
        String name,
        String description,
        List<PromptTemplate> templates
    ) {}

    record PromptTemplate(
        String id,
        String description,
        String template,
        List<String> requiredVariables,
        Map<String, String> examples
    ) {
        public String format(Map<String, Object> variables) {
            String result = template;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
            }
            return result;
        }
    }
}