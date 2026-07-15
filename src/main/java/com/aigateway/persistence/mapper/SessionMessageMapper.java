package com.aigateway.persistence.mapper;

import com.aigateway.model.Message;
import com.aigateway.model.ToolCall;
import com.aigateway.persistence.entity.SessionMessageEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionMessageMapper {

    private static final TypeReference<List<Map<String, Object>>> LIST_MAP = new TypeReference<>() {};
    private static final TypeReference<List<ToolCall>> LIST_TOOL_CALL = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public Message toModel(SessionMessageEntity entity) {
        List<ToolCall> toolCalls = null;
        if (entity.getToolCalls() != null) {
            try {
                String json = objectMapper.writeValueAsString(entity.getToolCalls());
                toolCalls = objectMapper.readValue(json, LIST_TOOL_CALL);
            } catch (Exception ex) {
                log.warn("session.mapper.toolcalls_parse_failed sessionId={} cause={}",
                        entity.getSessionId(), ex.getMessage());
            }
        }
        return Message.builder()
                .role(entity.getRole())
                .content(entity.getContent())
                .toolName(entity.getToolName())
                .toolCalls(toolCalls)
                .build();
    }

    public SessionMessageEntity toEntity(Message message, String sessionId,
                                         String agentId, int sequenceNum) {
        List<Map<String, Object>> toolCallsJson = null;
        if (message.getToolCalls() != null) {
            try {
                String json = objectMapper.writeValueAsString(message.getToolCalls());
                toolCallsJson = objectMapper.readValue(json, LIST_MAP);
            } catch (Exception ex) {
                log.warn("session.mapper.toolcalls_serialize_failed sessionId={} cause={}",
                        sessionId, ex.getMessage());
            }
        }
        return SessionMessageEntity.builder()
                .sessionId(sessionId)
                .sequenceNum(sequenceNum)
                .role(message.getRole())
                .content(message.getContent())
                .toolName(message.getToolName())
                .toolCalls(toolCallsJson)
                .agentId(agentId)
                .createdAt(Instant.now())
                .build();
    }
}
