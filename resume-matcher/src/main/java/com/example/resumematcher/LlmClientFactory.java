package com.example.resumematcher;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

public final class LlmClientFactory {

    public static final String ABACUS_ROUTELLM_BASE_URL = "https://routellm.abacus.ai/v1";

    private LlmClientFactory() {
    }

    public static OpenAIClient create() {
        String apiKey = System.getenv("ABACUS_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ABACUS_API_KEY is not set. Get a RouteLLM API key from abacus.ai and export it.");
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(ABACUS_ROUTELLM_BASE_URL)
                .build();
    }
}
