package com.aigateway.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Vendor-agnostic request sent to {@code POST /llm/chat}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {

    @NotBlank
    private String model;

    @NotEmpty
    @Valid
    private List<Message> messages;

    private Double temperature;

    // 🔥 ADD THIS (for future tool support)
    private List<ToolDefinition> tools;

    private String toolChoice; // "auto", "none"
}

