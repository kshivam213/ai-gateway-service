package com.aigateway.llm;

import com.aigateway.config.OpenAIProperties;
import com.aigateway.model.LLMRequest;
import com.aigateway.model.Message;
import com.aigateway.model.ToolCall;
import com.aigateway.model.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client over the OpenAI (and OpenAI-compatible) chat completions API.
 * <p>
 * Returns the assistant {@link Message} (text content and any {@code tool_calls}).
 * Higher-level concerns (retry, latency, request-id) live in {@link LLMService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestTemplate restTemplate;
    private final OpenAIProperties properties;

    public Message chatCompletion(LLMRequest request) {
        String url = properties.getBaseUrl() + CHAT_COMPLETIONS_PATH;

        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", toOpenAIMessages(request.getMessages()));
        body.put("temperature", request.getTemperature() == null ? 0.7 : request.getTemperature());
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", toOpenAITools(request.getTools()));
            body.put("tool_choice", request.getToolChoice() == null ? "auto" : request.getToolChoice());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return extractMessage(response.getBody());
    }

    private List<Map<String, Object>> toOpenAIMessages(List<Message> messages) {
        List<Map<String, Object>> out = new ArrayList<>(messages.size());
        for (Message m : messages) {
            Map<String, Object> obj = new HashMap<>();
            obj.put("role", m.getRole());
            obj.put("content", m.getContent());
            if ("assistant".equals(m.getRole()) && m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                List<Map<String, Object>> tcs = new ArrayList<>();
                for (ToolCall tc : m.getToolCalls()) {
                    tcs.add(Map.of(
                            "id", tc.getId(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tc.getName(),
                                    "arguments", tc.getArguments() == null ? "{}" : tc.getArguments())));
                }
                obj.put("tool_calls", tcs);
            }
            if ("tool".equals(m.getRole())) {
                if (m.getToolName() != null) {
                    obj.put("name", m.getToolName());
                }
                if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                    obj.put("tool_call_id", m.getToolCalls().get(0).getId());
                }
            }
            out.add(obj);
        }
        return out;
    }

    private List<Map<String, Object>> toOpenAITools(List<ToolDefinition> tools) {
        List<Map<String, Object>> out = new ArrayList<>(tools.size());
        for (ToolDefinition t : tools) {
            Map<String, Object> fn = new HashMap<>();
            fn.put("name", t.getName());
            if (t.getDescription() != null) {
                fn.put("description", t.getDescription());
            }
            fn.put("parameters", t.getParameters() == null
                    ? Map.of("type", "object", "properties", Map.of())
                    : t.getParameters());
            out.add(Map.of("type", "function", "function", fn));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Message extractMessage(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalStateException("Empty response from OpenAI");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("No choices in OpenAI response");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new IllegalStateException("No message in OpenAI choice");
        }
        Object content = message.get("content");
        List<ToolCall> toolCalls = null;
        Object rawCalls = message.get("tool_calls");
        if (rawCalls instanceof List<?> list && !list.isEmpty()) {
            toolCalls = new ArrayList<>(list.size());
            for (Object item : list) {
                Map<String, Object> tc = (Map<String, Object>) item;
                Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                String name = fn == null ? null : (String) fn.get("name");
                Object args = fn == null ? null : fn.get("arguments");
                toolCalls.add(ToolCall.builder()
                        .id((String) tc.get("id"))
                        .name(name)
                        .arguments(args == null ? "{}" : args.toString())
                        .build());
            }
        }
        return Message.builder()
                .role("assistant")
                .content(content == null ? "" : content.toString())
                .toolCalls(toolCalls)
                .build();
    }
}

