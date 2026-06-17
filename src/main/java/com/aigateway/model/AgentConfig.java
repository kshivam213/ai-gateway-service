package com.aigateway.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Persistent configuration for a registered agent. Tools are referenced by
 * name and resolved against the {@link com.aigateway.registry.ToolRegistry}
 * at execution time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    @NotBlank
    private String agentId;

    @NotBlank
    private String name;

    @NotBlank
    private String systemPrompt;

    @NotBlank
    private String model;

    @NotEmpty
    private List<String> tools;
}

