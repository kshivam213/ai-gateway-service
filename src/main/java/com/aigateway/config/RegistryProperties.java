package com.aigateway.config;

import com.aigateway.model.ToolConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "aigateway.registry")
public class RegistryProperties {
    private List<ToolConfig> seedTools = new ArrayList<>();
}
