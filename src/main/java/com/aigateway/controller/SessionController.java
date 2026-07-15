package com.aigateway.controller;

import com.aigateway.model.Message;
import com.aigateway.agent.SessionMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionMemory sessionMemory;

    @GetMapping("/{sessionId}/history")
    public List<Message> getHistory(@PathVariable String sessionId) {
        return sessionMemory.load(sessionId).stream()
                .filter(m -> !"system".equals(m.getRole()))
                .toList();
    }

    @DeleteMapping("/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        sessionMemory.clear(sessionId);
    }
}
