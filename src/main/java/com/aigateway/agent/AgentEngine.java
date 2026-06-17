package com.aigateway.agent;

import com.aigateway.llm.LLMService;
import com.aigateway.model.AgentConfig;
import com.aigateway.model.AgentRunResponse;
import com.aigateway.model.LLMRequest;
import com.aigateway.model.LLMResponse;
import com.aigateway.model.Message;
import com.aigateway.model.ToolCall;
import com.aigateway.model.ToolConfig;
import com.aigateway.model.ToolDefinition;
import com.aigateway.registry.ToolRegistry;
import com.aigateway.tool.ToolInvoker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core runtime that drives an agent through an iterative
 * {@code LLM → tool → LLM} loop.
 * <p>
 * On each iteration the assistant message is appended to the conversation. If
 * it contains {@code tool_calls}, every call is executed via {@link ToolInvoker}
 * and the results are appended as {@code role=tool} messages before the next
 * LLM call. The loop terminates as soon as the assistant returns a message
 * without tool calls, or after {@link #MAX_ITERATIONS} iterations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEngine {

    private static final int MAX_ITERATIONS = 5;

    private final LLMService llmService;
    private final ToolRegistry toolRegistry;
    private final ToolInvoker toolInvoker;
    private final ObjectMapper objectMapper;

    public AgentRunResponse run(AgentConfig agent, String input, Map<String, String> context) {
        return run(agent, input, context, new ArrayList<>());
    }

    /**
     * Session-aware variant. The supplied {@code history} is mutated in place:
     * the system prompt is prepended on first use, the new user turn and any
     * assistant/tool turns produced during the loop are appended. Callers that
     * persist sessions can read the final state directly from {@code history}.
     */
    public AgentRunResponse run(AgentConfig agent, String input, Map<String, String> context,
                                List<Message> history) {
        if (history.isEmpty()) {
            history.add(Message.builder().role("system").content(agent.getSystemPrompt()).build());
        }
        history.add(Message.builder().role("user").content(input).build());
        List<Message> messages = history;
        Map<String, String> safeContext = context == null ? Map.of() : context;

        List<ToolDefinition> toolDefinitions = buildToolDefinitions(agent);

        String lastRequestId = null;
        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            LLMRequest request = LLMRequest.builder()
                    .model(agent.getModel())
                    .messages(messages)
                    .tools(toolDefinitions.isEmpty() ? null : toolDefinitions)
                    .toolChoice(toolDefinitions.isEmpty() ? null : "auto")
                    .build();

            LLMResponse response = llmService.chat(request);
            lastRequestId = response.getRequestId();
            Message assistant = response.getChoices().get(0).getMessage();
            messages.add(assistant);

            List<ToolCall> toolCalls = assistant.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                log.info("agent.engine.done agentId={} iteration={} requestId={}",
                        agent.getAgentId(), iteration, lastRequestId);
                return AgentRunResponse.builder()
                        .response(assistant.getContent() == null ? "" : assistant.getContent())
                        .requestId(lastRequestId)
                        .build();
            }

            log.info("agent.engine.tool_calls agentId={} iteration={} requestId={} count={}",
                    agent.getAgentId(), iteration, lastRequestId, toolCalls.size());
            for (ToolCall call : toolCalls) {
                messages.add(executeToolCall(agent, call, safeContext));
            }
        }

        log.warn("agent.engine.max_iterations agentId={} requestId={}",
                agent.getAgentId(), lastRequestId);
        Message tail = messages.get(messages.size() - 1);
        return AgentRunResponse.builder()
                .response(tail.getContent() == null
                        ? "Agent did not converge within " + MAX_ITERATIONS + " iterations."
                        : tail.getContent())
                .requestId(lastRequestId)
                .build();
    }

    private Message executeToolCall(AgentConfig agent, ToolCall call, Map<String, String> context) {
        ToolConfig toolConfig = toolRegistry.getTool(call.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Agent '" + agent.getAgentId() + "' attempted to call unknown tool '"
                                + call.getName() + "'"));

        Object result = toolInvoker.invoke(call, toolConfig, context);
        String content = serialize(result);
        return Message.builder()
                .role("tool")
                .toolName(call.getName())
                .toolCalls(List.of(call))
                .content(content)
                .build();
    }

    private List<ToolDefinition> buildToolDefinitions(AgentConfig agent) {
        List<ToolDefinition> defs = new ArrayList<>();
        for (String toolName : agent.getTools()) {
            toolRegistry.getTool(toolName).ifPresent(tc -> defs.add(toToolDefinition(tc)));
        }
        return defs;
    }

    private ToolDefinition toToolDefinition(ToolConfig tc) {
        Map<String, Object> parameters = (tc.getParameters() == null || tc.getParameters().isEmpty())
                ? Map.of("type", "object", "properties", Map.of())
                : tc.getParameters();
        String description = (tc.getDescription() == null || tc.getDescription().isBlank())
                ? tc.getName()
                : tc.getDescription();
        return new ToolDefinition(tc.getName(), description, parameters);
    }

    private String serialize(Object result) {
        if (result == null) {
            return "{}";
        }
        if (result instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            log.warn("agent.engine.serialize_failed cause={}", ex.getMessage());
            return String.valueOf(result);
        }
    }
}

