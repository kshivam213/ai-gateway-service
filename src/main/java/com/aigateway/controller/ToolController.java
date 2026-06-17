package com.aigateway.controller;

import com.aigateway.model.ToolConfig;
import com.aigateway.registry.ToolRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolRegistry toolRegistry;

    @GetMapping
    public Collection<ToolConfig> listTools() {
        return toolRegistry.getAllTools();
    }

    @GetMapping("/{name}")
    public ResponseEntity<ToolConfig> getTool(@PathVariable String name) {
        return toolRegistry.getTool(name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ToolConfig registerTool(@Valid @RequestBody ToolConfig toolConfig) {
        return toolRegistry.registerTool(toolConfig);
    }
}
