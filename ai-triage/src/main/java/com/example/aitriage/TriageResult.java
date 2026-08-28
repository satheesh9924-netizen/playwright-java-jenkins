package com.example.aitriage;

public record TriageResult(
        String category,
        String summary,
        String suggestedAction
) {
    public static final java.util.List<String> VALID_CATEGORIES =
            java.util.List.of("FLAKY", "REGRESSION", "ENVIRONMENT", "UNKNOWN");
}
