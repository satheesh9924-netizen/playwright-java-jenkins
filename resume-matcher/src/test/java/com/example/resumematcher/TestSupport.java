package com.example.resumematcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class TestSupport {

    public static final OpenAIClient CLIENT = LlmClientFactory.create();
    public static final ResumeMatcher MATCHER = new ResumeMatcher(CLIENT);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestSupport() {
    }

    public static String readGoldenFile(String caseName, String fileName) {
        String path = "/golden/" + caseName + "/" + fileName;
        try (InputStream in = TestSupport.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test fixture: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public record ExpectedRange(int minScore, int maxScore, String verdict) {
    }

    public static ExpectedRange readExpected(String caseName) {
        String json = readGoldenFile(caseName, "expected.json");
        try {
            return MAPPER.readValue(json, ExpectedRange.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
