package com.aigateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single chat message exchanged with an LLM.
 * <p>
 * {@code role} is one of: {@code system}, {@code user}, {@code assistant}, {@code tool}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String role; // user, assistant, tool
    private String content;

    // 🔥 for tool calling
    private List<ToolCall> toolCalls;

    // 🔥 for tool response
    private String toolName;
}