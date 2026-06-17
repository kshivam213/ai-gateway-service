package com.aigateway.controller;

import com.aigateway.agent.AgentService;
import com.aigateway.model.AgentConfig;
import com.aigateway.model.AgentRunRequest;
import com.aigateway.model.AgentRunResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentConfig createAgent(@Valid @RequestBody AgentConfig config) {
        return agentService.createAgent(config);
    }

    @GetMapping
    public Collection<AgentConfig> getAllAgents() {
        return agentService.getAllAgents();
    }

    @GetMapping("/{agentId}")
    public AgentConfig getAgent(@PathVariable String agentId) {
        return agentService.getAgent(agentId);
    }

    @PostMapping("/run")
    public AgentRunResponse run(@Valid @RequestBody AgentRunRequest request) {
        return agentService.run(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}

