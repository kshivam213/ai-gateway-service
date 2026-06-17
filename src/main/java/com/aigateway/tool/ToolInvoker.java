package com.aigateway.tool;

import com.aigateway.model.ToolCall;
import com.aigateway.model.ToolConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Invokes a registered tool by issuing the configured HTTP request and
 * forwarding the LLM-supplied JSON arguments as the request body.
 * <p>
 * Headers are resolved through {@link HeaderResolver} from the runtime
 * {@code context} map, allowing per-request values such as {@code userId} or
 * {@code merchantId} to be injected without ever being baked into the stored
 * {@link ToolConfig}. The raw response body is returned for the caller to embed
 * in a follow-up LLM message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolInvoker {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HeaderResolver headerResolver;

    public Object invoke(ToolCall toolCall, ToolConfig toolConfig, Map<String, String> context) {
        Map<String, Object> arguments = parseArguments(toolCall.getArguments());
        applyDefaultParams(arguments, toolConfig.getDefaultParams());

        HttpHeaders httpHeaders = buildHeaders(toolConfig, context);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(arguments, httpHeaders);

        HttpMethod method = HttpMethod.valueOf(toolConfig.getMethod().toUpperCase());
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    toolConfig.getUrl(), method, entity, Object.class);
            long latency = System.currentTimeMillis() - start;
            log.info("tool.invoke name={} method={} url={} status={} latencyMs={}",
                    toolConfig.getName(), method, toolConfig.getUrl(),
                    response.getStatusCode().value(), latency);
            return response.getBody();
        } catch (RestClientException ex) {
            long latency = System.currentTimeMillis() - start;
            log.error("tool.invoke.failed name={} method={} url={} latencyMs={} cause={}",
                    toolConfig.getName(), method, toolConfig.getUrl(), latency, ex.getMessage());
            return Map.of("error", ex.getMessage());
        }
    }

    private HttpHeaders buildHeaders(ToolConfig toolConfig, Map<String, String> context) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        Map<String, String> resolved = headerResolver.resolve(toolConfig.getHeaders(), context);
        resolved.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                httpHeaders.set(k, v);
            }
        });
        return httpHeaders;
    }

    private void applyDefaultParams(Map<String, Object> arguments, Map<String, Object> defaults) {
        if (defaults == null || defaults.isEmpty()) {
            return;
        }
        defaults.forEach(arguments::putIfAbsent);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(objectMapper.readValue(arguments, Map.class));
        } catch (JsonProcessingException ex) {
            log.warn("tool.invoke.bad_arguments raw={} cause={}", arguments, ex.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("_raw", arguments);
            return fallback;
        }
    }
}

