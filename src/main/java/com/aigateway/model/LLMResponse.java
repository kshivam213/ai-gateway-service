package com.aigateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Vendor-agnostic response returned by the LLM gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    private String requestId;
    private String model;
    private String provider;
    private long latencyMs;

    private List<Choice> choices;

    private Usage usage;
}