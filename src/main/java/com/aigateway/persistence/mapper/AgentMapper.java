package com.aigateway.persistence.mapper;

import com.aigateway.model.AgentConfig;
import com.aigateway.persistence.entity.AgentEntity;
import com.aigateway.persistence.entity.AgentToolEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AgentMapper {

    public AgentConfig toModel(AgentEntity entity) {
        List<String> tools = entity.getAgentTools().stream()
                .map(AgentToolEntity::getToolName)
                .toList();
        return AgentConfig.builder()
                .agentId(entity.getAgentId())
                .name(entity.getName())
                .systemPrompt(entity.getSystemPrompt())
                .model(entity.getModel())
                .tools(tools)
                .build();
    }

    public AgentEntity toEntity(AgentConfig model) {
        Instant now = Instant.now();
        AgentEntity entity = AgentEntity.builder()
                .agentId(model.getAgentId())
                .name(model.getName())
                .systemPrompt(model.getSystemPrompt())
                .model(model.getModel())
                .createdAt(now)
                .updatedAt(now)
                .build();

        List<AgentToolEntity> agentTools = model.getTools().stream()
                .map(toolName -> AgentToolEntity.builder()
                        .agentId(model.getAgentId())
                        .toolName(toolName)
                        .build())
                .toList();
        entity.setAgentTools(agentTools);
        return entity;
    }
}
