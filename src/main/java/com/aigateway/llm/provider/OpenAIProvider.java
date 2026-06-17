package com.aigateway.llm.provider;

import com.aigateway.llm.OpenAIClient;
import com.aigateway.model.LLMRequest;
import com.aigateway.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAIProvider implements LLMProvider {

    private final OpenAIClient client;

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public boolean supports(String model) {
        if (model == null) {
            return false;
        }
        String m = model.toLowerCase();
        return m.startsWith("gpt-") || m.startsWith("o1") || m.startsWith("o3") || m.startsWith("openai/");
    }

    @Override
    public Message chat(LLMRequest request) {
        return client.chatCompletion(request);
    }
}
