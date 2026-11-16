package com.uniremington.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        String apiRevision,
        long timeoutSeconds,
        int maxInputChars,
        String systemInstruction
) {
}
