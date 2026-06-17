package com.aigateway.registry;

import com.aigateway.config.RegistryProperties;
import com.aigateway.model.ToolConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry of tools the agent engine can invoke. Pre-populated from
 * {@link RegistryProperties} at startup; new tools can be added via
 * {@link #registerTool(ToolConfig)}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolRegistry {

    private final ConcurrentMap<String, ToolConfig> tools = new ConcurrentHashMap<>();
    private final RegistryProperties properties;

    @PostConstruct
    void seed() {
        if (properties.getSeedTools() == null) {
            return;
        }
        for (ToolConfig tool : properties.getSeedTools()) {
            tools.put(tool.getName(), tool);
            log.info("tool.registry.seed name={} url={} method={}",
                    tool.getName(), tool.getUrl(), tool.getMethod());
        }
    }

    public ToolConfig registerTool(ToolConfig tool) {
        tools.put(tool.getName(), tool);
        log.info("tool.registry.register name={} url={} method={}",
                tool.getName(), tool.getUrl(), tool.getMethod());
        return tool;
    }

    public Optional<ToolConfig> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<ToolConfig> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }
}
