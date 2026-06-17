package com.aigateway.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class AgentRunRequest {

    @NotBlank
    private String agentId;

    @NotBlank
    private String input;

    private Map<String, String> context;

    private Map<String, String> metadata;
}

