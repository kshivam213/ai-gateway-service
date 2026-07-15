package com.aigateway.registry;

import com.aigateway.config.RegistryProperties;
import com.aigateway.model.ToolConfig;
import com.aigateway.persistence.mapper.ToolMapper;
import com.aigateway.persistence.repository.ToolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Write-through cache over the {@code aigateway.tools} table.
 * Reads always hit the in-memory map; mutations persist to DB first.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolRegistry {

    private final ConcurrentMap<String, ToolConfig> cache = new ConcurrentHashMap<>();

    private final ToolRepository toolRepository;
    private final ToolMapper toolMapper;
    private final RegistryProperties properties;

    @PostConstruct
    @Transactional
    void init() {
        toolRepository.findAll().forEach(entity -> {
            ToolConfig tool = toolMapper.toModel(entity);
            cache.put(tool.getName(), tool);
            log.info("tool.registry.load name={}", tool.getName());
        });

        if (properties.getSeedTools() != null) {
            for (ToolConfig tool : properties.getSeedTools()) {
                if (!cache.containsKey(tool.getName())) {
                    registerTool(tool);
                }
            }
        }
    }

    @Transactional
    public ToolConfig registerTool(ToolConfig tool) {
        toolRepository.save(toolMapper.toEntity(tool));
        cache.put(tool.getName(), tool);
        log.info("tool.registry.register name={} url={} method={}",
                tool.getName(), tool.getUrl(), tool.getMethod());
        return tool;
    }

    public Optional<ToolConfig> getTool(String name) {
        return Optional.ofNullable(cache.get(name));
    }

    public Collection<ToolConfig> getAllTools() {
        return Collections.unmodifiableCollection(cache.values());
    }
}
