package com.aigateway.agent;

import com.aigateway.model.AgentRunResponse;
import com.aigateway.model.ToolCall;
import com.aigateway.persistence.entity.AgentRunEntity;
import com.aigateway.persistence.repository.AgentRunRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Persists every agent invocation to {@code aigateway.agent_runs}.
 * Called by {@link AgentService} after each run completes or fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunService {

    private static final TypeReference<List<Map<String, Object>>> LIST_MAP = new TypeReference<>() {};

    private final AgentRunRepository agentRunRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void persist(String agentId, String sessionId, String input,
                        AgentRunResponse response, List<ToolCall> toolCalls, long latencyMs) {
        List<Map<String, Object>> toolCallsJson = null;
        if (toolCalls != null && !toolCalls.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(toolCalls);
                toolCallsJson = objectMapper.readValue(json, LIST_MAP);
            } catch (Exception ex) {
                log.warn("agent.run.serialize_tool_calls agentId={} cause={}", agentId, ex.getMessage());
            }
        }

        AgentRunEntity entity = AgentRunEntity.builder()
                .requestId(response.getRequestId())
                .agentId(agentId)
                .sessionId(sessionId)
                .input(input)
                .response(response.getResponse())
                .toolCalls(toolCallsJson)
                .promptTokens(response.getPromptTokens())
                .completionTokens(response.getCompletionTokens())
                .totalTokens(response.getTotalTokens())
                .latencyMs(latencyMs)
                .iterations(response.getIterations() == null ? null : response.getIterations().shortValue())
                .status(response.getStatus() != null ? response.getStatus() : "COMPLETED")
                .createdAt(Instant.now())
                .build();

        agentRunRepository.save(entity);
        log.info("agent.run.persisted requestId={} agentId={} status={} tokens={}",
                entity.getRequestId(), agentId, entity.getStatus(), entity.getTotalTokens());
    }

    @Transactional
    public void persistFailure(String agentId, String sessionId, String input,
                               String requestId, String errorMessage, long latencyMs) {
        AgentRunEntity entity = AgentRunEntity.builder()
                .requestId(requestId)
                .agentId(agentId)
                .sessionId(sessionId)
                .input(input)
                .status("FAILED")
                .errorMessage(errorMessage)
                .latencyMs(latencyMs)
                .createdAt(Instant.now())
                .build();

        agentRunRepository.save(entity);
        log.warn("agent.run.failed requestId={} agentId={} error={}", requestId, agentId, errorMessage);
    }
}
