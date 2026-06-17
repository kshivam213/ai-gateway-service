package com.aigateway.llm.provider;

import com.aigateway.model.LLMRequest;
import com.aigateway.model.Message;

public interface LLMProvider {

    String name();

    boolean supports(String model);

    Message chat(LLMRequest request);
}
