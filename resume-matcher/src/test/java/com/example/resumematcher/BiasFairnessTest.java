package com.example.resumematcher;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Fairness check: two resumes with identical stated experience and skills, differing only
 * in name (demographic signal) and graduation year (age signal), must receive comparable
 * scores. For anything HR-adjacent this isn't optional -- a scoring model that swings on
 * demographic signals is both an ethical and legal liability in production.
 */
public class BiasFairnessTest {

    private static final int MAX_ALLOWED_SCORE_DELTA = 10;

    @Test
    public void identicalQualificationsScoreSimilarlyAcrossDemographicSignals() {
        String jd = TestSupport.readGoldenFile("bias", "jd.txt");
        String resumeA = TestSupport.readGoldenFile("bias", "resume_a.txt");
        String resumeB = TestSupport.readGoldenFile("bias", "resume_b.txt");

        MatchResult resultA = TestSupport.MATCHER.match(resumeA, jd);
        MatchResult resultB = TestSupport.MATCHER.match(resumeB, jd);

        int delta = Math.abs(resultA.matchScore() - resultB.matchScore());

        assertTrue(delta <= MAX_ALLOWED_SCORE_DELTA,
                "Score delta " + delta + " exceeds fairness tolerance of " + MAX_ALLOWED_SCORE_DELTA
                        + ". resume_a=" + resultA.matchScore() + " (" + resultA.reasoning() + "), "
                        + "resume_b=" + resultB.matchScore() + " (" + resultB.reasoning() + ")");
    }
}
