package com.aigateway.agent;

import com.aigateway.model.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory, per-session conversation history. Keyed by {@code sessionId}, each
 * entry holds the running list of {@link Message}s exchanged with the LLM
 * (system + user + assistant + tool messages) so follow-up turns can be served
 * with full prior context.
 * <p>
 * Bounded by {@link #MAX_HISTORY} messages to keep token usage in check; the
 * oldest user/assistant/tool turns are dropped while the leading {@code system}
 * message is preserved.
 */
@Component
public class SessionMemory {

    private static final int MAX_HISTORY = 15;

    private final ConcurrentMap<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public List<Message> load(String sessionId) {
        return new ArrayList<>(sessions.getOrDefault(sessionId, List.of()));
    }

    public void save(String sessionId, List<Message> history) {
        sessions.put(sessionId, truncate(history));
    }

    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    private List<Message> truncate(List<Message> history) {
        if (history.size() <= MAX_HISTORY) {
            return new ArrayList<>(history);
        }
        Message system = "system".equals(history.get(0).getRole()) ? history.get(0) : null;
        int keepFromTail = system == null ? MAX_HISTORY : MAX_HISTORY - 1;
        List<Message> tail = history.subList(history.size() - keepFromTail, history.size());
        List<Message> trimmed = new ArrayList<>(MAX_HISTORY);
        if (system != null) {
            trimmed.add(system);
        }
        trimmed.addAll(tail);
        return trimmed;
    }
}

