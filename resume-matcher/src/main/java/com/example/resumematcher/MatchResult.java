package com.example.resumematcher;

import java.util.List;

public record MatchResult(
        int matchScore,
        String verdict,
        List<String> matchedSkills,
        List<String> missingSkills,
        String reasoning
) {
    public static final List<String> VALID_VERDICTS = List.of("STRONG_FIT", "PARTIAL_FIT", "NOT_A_FIT");
}
