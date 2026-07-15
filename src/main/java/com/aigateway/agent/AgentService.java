package com.aigateway.agent;

import com.aigateway.model.AgentConfig;
import com.aigateway.model.AgentRunRequest;
import com.aigateway.model.AgentRunResponse;
import com.aigateway.model.Message;
import com.aigateway.model.ToolCall;
import com.aigateway.util.RequestIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Façade between the HTTP layer and the {@link AgentEngine}.
 * Owns the run lifecycle: session load → engine execution → session save → run persistence.
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
    private final AgentRunService agentRunService;

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

        long start = System.currentTimeMillis();
        try {
            AgentRunResponse response = agentEngine.run(agent, request.getInput(), context, history);
            long latencyMs = System.currentTimeMillis() - start;
            response.setLatencyMs(latencyMs);

            sessionMemory.save(sessionId, agent.getAgentId(), history);

            List<ToolCall> toolCalls = extractToolCalls(history);
            agentRunService.persist(agent.getAgentId(), sessionId, request.getInput(),
                    response, toolCalls, latencyMs);

            return response;
        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - start;
            String fallbackId = RequestIdUtil.newRequestId();
            agentRunService.persistFailure(agent.getAgentId(), sessionId, request.getInput(),
                    fallbackId, ex.getMessage(), latencyMs);
            throw ex;
        }
    }

    private List<ToolCall> extractToolCalls(List<Message> history) {
        return history.stream()
                .filter(m -> m.getToolCalls() != null)
                .flatMap(m -> m.getToolCalls().stream())
                .toList();
    }
}

