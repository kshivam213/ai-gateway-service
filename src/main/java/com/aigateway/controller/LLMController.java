package com.aigateway.controller;

import com.aigateway.llm.LLMService;
import com.aigateway.model.LLMRequest;
import com.aigateway.model.LLMResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LLMController {

    private final LLMService llmService;

    @PostMapping("/chat")
    public LLMResponse chat(@Valid @RequestBody LLMRequest request) {
        return llmService.chat(request);
    }
}
