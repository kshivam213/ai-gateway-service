package com.aigateway.persistence.mapper;

import com.aigateway.model.ToolConfig;
import com.aigateway.persistence.entity.ToolEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ToolMapper {

    public ToolConfig toModel(ToolEntity entity) {
        return ToolConfig.builder()
                .name(entity.getName())
                .url(entity.getUrl())
                .method(entity.getMethod())
                .description(entity.getDescription())
                .parameters(entity.getParameters())
                .headers(entity.getHeaders())
                .defaultParams(entity.getDefaultParams())
                .build();
    }

    public ToolEntity toEntity(ToolConfig model) {
        Instant now = Instant.now();
        return ToolEntity.builder()
                .name(model.getName())
                .url(model.getUrl())
                .method(model.getMethod())
                .description(model.getDescription())
                .parameters(model.getParameters())
                .headers(model.getHeaders())
                .defaultParams(model.getDefaultParams())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
