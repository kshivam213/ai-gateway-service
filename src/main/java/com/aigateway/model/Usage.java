package com.aigateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usage {

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}