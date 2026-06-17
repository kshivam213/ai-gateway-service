package com.aigateway.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Definition of a tool the agent engine can invoke.
 * <p>
 * {@link #headers} may contain {@code {{placeholder}}} tokens that are
 * substituted at invocation time from the runtime context (e.g. {@code userId},
 * {@code merchantId}). User-specific values must never be stored here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolConfig {

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    @NotBlank
    private String method;

    private String description;

    private Map<String, Object> parameters;

    private Map<String, String> headers;

    private Map<String, Object> defaultParams;
}
