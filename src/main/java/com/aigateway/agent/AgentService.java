package com.aigateway.agent;

import com.aigateway.model.AgentConfig;
import com.aigateway.model.AgentRunRequest;
import com.aigateway.model.AgentRunResponse;
import com.aigateway.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Façade between the HTTP layer and the {@link AgentEngine}. Resolves the
 * agent configuration from the registry and delegates execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final String DEFAULT_SESSION_ID = "default";
    private static final String SESSION_ID_KEY = "sessionId";

    private final AgentRegistry agentRegistry;
    private final AgentEngine agentEngine;
    private final SessionMemory sessionMemory;

    public AgentConfig createAgent(AgentConfig config) {
        return agentRegistry.createAgent(config);
    }

    public Collection<AgentConfig> getAllAgents() {
        return agentRegistry.getAllAgents();
    }

    public AgentConfig getAgent(String agentId) {
        return agentRegistry.getAgent(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agentId));
    }

    public AgentRunResponse run(AgentRunRequest request) {
        AgentConfig agent = getAgent(request.getAgentId());
        Map<String, String> context = request.getContext() == null ? Map.of() : request.getContext();
        Map<String, String> metadata = request.getMetadata() == null ? Map.of() : request.getMetadata();
        String sessionId = metadata.getOrDefault(SESSION_ID_KEY, DEFAULT_SESSION_ID);

        List<Message> history = sessionMemory.load(sessionId);
        log.info("agent.run agentId={} model={} sessionId={} historySize={} contextKeys={}",
                agent.getAgentId(), agent.getModel(), sessionId, history.size(), context.keySet());

        AgentRunResponse response = agentEngine.run(agent, request.getInput(), context, history);
        sessionMemory.save(sessionId, history);
        return response;
    }
}

