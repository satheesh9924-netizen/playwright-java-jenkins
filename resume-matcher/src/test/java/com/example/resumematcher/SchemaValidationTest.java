package com.example.resumematcher;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Validates that Claude's output conforms to the required structure, regardless of whether
 * the match verdict itself is "correct" -- an LLM can be well-calibrated and still return
 * malformed output under load, so structure is tested independently of accuracy.
 */
public class SchemaValidationTest {

    @Test
    public void matchResultHasWellFormedFields() {
        String resume = TestSupport.readGoldenFile("case1_strong_fit", "resume.txt");
        String jd = TestSupport.readGoldenFile("case1_strong_fit", "jd.txt");

        MatchResult result = TestSupport.MATCHER.match(resume, jd);

        assertTrue(result.matchScore() >= 0 && result.matchScore() <= 100,
                "matchScore out of range: " + result.matchScore());
        assertTrue(MatchResult.VALID_VERDICTS.contains(result.verdict()),
                "Unexpected verdict: " + result.verdict());
        assertNotNull(result.matchedSkills());
        assertNotNull(result.missingSkills());
        assertNotNull(result.reasoning());
        assertFalse(result.reasoning().isBlank());
    }
}
