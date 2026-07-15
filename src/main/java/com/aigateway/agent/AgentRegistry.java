package com.aigateway.agent;

import com.aigateway.model.AgentConfig;
import com.aigateway.persistence.entity.AgentToolEntity;
import com.aigateway.persistence.mapper.AgentMapper;
import com.aigateway.persistence.repository.AgentRepository;
import com.aigateway.persistence.repository.AgentToolRepository;
import com.aigateway.registry.ToolRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Write-through cache over the {@code aigateway.agents} table.
 * Validates that every tool referenced by an agent exists in the
 * {@link ToolRegistry} before accepting the registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRegistry {

    private final ConcurrentMap<String, AgentConfig> cache = new ConcurrentHashMap<>();

    private final AgentRepository agentRepository;
    private final AgentToolRepository agentToolRepository;
    private final AgentMapper agentMapper;
    private final ToolRegistry toolRegistry;

    @PostConstruct
    @Transactional(readOnly = true)
    void init() {
        agentRepository.findAll().forEach(entity -> {
            List<AgentToolEntity> tools = agentToolRepository.findByAgentId(entity.getAgentId());
            entity.setAgentTools(tools);
            AgentConfig agent = agentMapper.toModel(entity);
            cache.put(agent.getAgentId(), agent);
            log.info("agent.registry.load agentId={} tools={}", agent.getAgentId(), agent.getTools());
        });
    }

    @Transactional
    public AgentConfig createAgent(AgentConfig config) {
        validate(config);
        if (cache.containsKey(config.getAgentId())) {
            throw new IllegalStateException("Agent already exists: " + config.getAgentId());
        }
        var entity = agentMapper.toEntity(config);
        agentRepository.save(entity);
        agentToolRepository.saveAll(entity.getAgentTools());
        cache.put(config.getAgentId(), config);
        log.info("agent.registry.create agentId={} model={} tools={}",
                config.getAgentId(), config.getModel(), config.getTools());
        return config;
    }

    public Optional<AgentConfig> getAgent(String agentId) {
        return Optional.ofNullable(cache.get(agentId));
    }

    public Collection<AgentConfig> getAllAgents() {
        return Collections.unmodifiableCollection(cache.values());
    }

    private void validate(AgentConfig config) {
        for (String toolName : config.getTools()) {
            if (toolRegistry.getTool(toolName).isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown tool '" + toolName + "' for agent '" + config.getAgentId() + "'");
            }
        }
    }
}

