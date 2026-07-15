package com.aigateway.agent;

import com.aigateway.model.Message;
import com.aigateway.persistence.mapper.SessionMessageMapper;
import com.aigateway.persistence.repository.SessionMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Persistent session memory backed by {@code aigateway.session_messages}.
 * Applies the same MAX_HISTORY truncation as the old in-memory version:
 * the leading system message is always preserved; oldest turns are dropped
 * from the tail when the window is full.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionMemory {

    private static final int MAX_HISTORY = 15;

    private final SessionMessageRepository sessionMessageRepository;
    private final SessionMessageMapper sessionMessageMapper;

    @Transactional(readOnly = true)
    public List<Message> load(String sessionId) {
        List<Message> history = sessionMessageRepository
                .findBySessionIdOrderBySequenceNumAsc(sessionId)
                .stream()
                .map(sessionMessageMapper::toModel)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        log.debug("session.memory.load sessionId={} size={}", sessionId, history.size());
        return history;
    }

    @Transactional
    public void save(String sessionId, String agentId, List<Message> history) {
        List<Message> toSave = truncate(history);
        sessionMessageRepository.deleteBySessionId(sessionId);

        AtomicInteger seq = new AtomicInteger(0);
        toSave.forEach(message -> sessionMessageRepository.save(
                sessionMessageMapper.toEntity(message, sessionId, agentId, seq.incrementAndGet())
        ));
        log.debug("session.memory.save sessionId={} messages={}", sessionId, toSave.size());
    }

    @Transactional
    public void clear(String sessionId) {
        sessionMessageRepository.deleteBySessionId(sessionId);
        log.info("session.memory.clear sessionId={}", sessionId);
    }

    private List<Message> truncate(List<Message> history) {
        if (history.size() <= MAX_HISTORY) {
            return new ArrayList<>(history);
        }
        Message system = "system".equals(history.get(0).getRole()) ? history.get(0) : null;
        int keepFromTail = system == null ? MAX_HISTORY : MAX_HISTORY - 1;
        List<Message> tail = history.subList(history.size() - keepFromTail, history.size());
        List<Message> trimmed = new ArrayList<>(MAX_HISTORY);
        if (system != null) trimmed.add(system);
        trimmed.addAll(tail);
        return trimmed;
    }
}
