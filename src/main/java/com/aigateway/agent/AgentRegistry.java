package com.aigateway.agent;

import com.aigateway.model.AgentConfig;
import com.aigateway.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory, thread-safe registry of {@link AgentConfig}s.
 * <p>
 * Validates that every tool referenced by an agent exists in the
 * {@link ToolRegistry} before accepting the registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRegistry {

    private final ConcurrentMap<String, AgentConfig> agents = new ConcurrentHashMap<>();
    private final ToolRegistry toolRegistry;

    public AgentConfig createAgent(AgentConfig config) {
        validate(config);
        AgentConfig existing = agents.putIfAbsent(config.getAgentId(), config);
        if (existing != null) {
            throw new IllegalStateException("Agent already exists: " + config.getAgentId());
        }
        log.info("agent.registry.create agentId={} model={} tools={}",
                config.getAgentId(), config.getModel(), config.getTools());
        return config;
    }

    public Optional<AgentConfig> getAgent(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    public Collection<AgentConfig> getAllAgents() {
        return Collections.unmodifiableCollection(agents.values());
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

