package com.aigateway.llm;

import com.aigateway.llm.provider.LLMProvider;
import com.aigateway.model.Choice;
import com.aigateway.model.LLMRequest;
import com.aigateway.model.LLMResponse;
import com.aigateway.model.Message;
import com.aigateway.model.Usage;
import com.aigateway.util.RequestIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vendor-agnostic façade over the registered {@link LLMProvider} strategies.
 * <p>
 * Resolves the provider from the request's model id, measures latency, retries
 * once on failure, and emits structured observability logs.
 */
@Slf4j
@Service
public class LLMService {

    private final List<LLMProvider> providers;

    public LLMService(List<LLMProvider> providers) {
        this.providers = providers;
    }

    public LLMResponse chat(LLMRequest request) {
        String requestId = RequestIdUtil.newRequestId();
        LLMProvider provider = resolveProvider(request.getModel());

        long start = System.currentTimeMillis();
        try {
            Message assistant = invokeWithRetry(provider, request, requestId);
            long latency = System.currentTimeMillis() - start;
            String finishReason = (assistant.getToolCalls() != null && !assistant.getToolCalls().isEmpty())
                    ? "tool_calls" : "stop";
            log.info("llm.chat requestId={} provider={} model={} latencyMs={} status=ok finishReason={}",
                    requestId, provider.name(), request.getModel(), latency, finishReason);
            return LLMResponse.builder()
                    .requestId(requestId)
                    .model(request.getModel())
                    .provider(provider.name())
                    .choices(List.of(Choice.builder()
                            .index(0)
                            .message(assistant)
                            .finishReason(finishReason)
                            .build()))
                    .usage(Usage.builder().build())
                    .latencyMs(latency)
                    .build();
        } catch (RuntimeException ex) {
            long latency = System.currentTimeMillis() - start;
            log.error("llm.chat requestId={} provider={} model={} latencyMs={} status=error cause={}",
                    requestId, provider.name(), request.getModel(), latency, ex.getMessage());
            throw ex;
        }
    }

    private Message invokeWithRetry(LLMProvider provider, LLMRequest request, String requestId) {
        try {
            return provider.chat(request);
        } catch (RuntimeException firstFailure) {
            log.warn("llm.chat.retry requestId={} provider={} cause={}",
                    requestId, provider.name(), firstFailure.getMessage());
            try {
                return provider.chat(request);
            } catch (RuntimeException secondFailure) {
                log.error("llm.chat.failed requestId={} provider={} cause={}",
                        requestId, provider.name(), secondFailure.getMessage());
                throw secondFailure;
            }
        }
    }

    private LLMProvider resolveProvider(String model) {
        return providers.stream()
                .filter(p -> p.supports(model))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No LLM provider supports model: " + model));
    }
}
