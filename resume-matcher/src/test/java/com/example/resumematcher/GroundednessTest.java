package com.example.resumematcher;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.testng.Assert.assertTrue;

/**
 * Checks that the model's claims are grounded in the source documents rather than invented.
 * A "matched" skill must share at least one meaningful keyword with the resume; a "missing"
 * skill must share none. Token-overlap (not whole-phrase substring) tolerates the model
 * paraphrasing a skill slightly while still catching genuinely fabricated skills, which share
 * zero real overlap with the source text.
 */
public class GroundednessTest {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "or", "of", "in", "on", "for", "with", "to", "the", "is", "are",
            "was", "were", "be", "been", "this", "that", "using", "use", "based", "level",
            "experience", "professional", "hands", "skills", "tools", "exposure", "familiar",
            "familiarity", "knowledge", "years", "year");

    @Test
    public void matchedSkillsAreGroundedInResumeText() {
        String resume = TestSupport.readGoldenFile("case1_strong_fit", "resume.txt");
        String jd = TestSupport.readGoldenFile("case1_strong_fit", "jd.txt");
        String resumeLower = resume.toLowerCase(Locale.ROOT);

        MatchResult result = TestSupport.MATCHER.match(resume, jd);

        for (String skill : result.matchedSkills()) {
            List<String> tokens = significantTokens(skill);
            boolean anyTokenPresent = tokens.isEmpty()
                    || tokens.stream().anyMatch(resumeLower::contains);
            assertTrue(anyTokenPresent,
                    "Claimed matched skill \"" + skill + "\" shares no keyword with the resume text -- "
                            + "possible hallucination. Full reasoning: " + result.reasoning());
        }
    }

    @Test
    public void missingSkillsAreNotAlreadyPresentInResume() {
        String resume = TestSupport.readGoldenFile("case3_partial_fit", "resume.txt");
        String jd = TestSupport.readGoldenFile("case3_partial_fit", "jd.txt");
        String resumeLower = resume.toLowerCase(Locale.ROOT);

        MatchResult result = TestSupport.MATCHER.match(resume, jd);

        for (String skill : result.missingSkills()) {
            List<String> tokens = significantTokens(skill);
            boolean anyTokenPresent = tokens.stream().anyMatch(resumeLower::contains);
            assertTrue(!anyTokenPresent,
                    "Skill \"" + skill + "\" was listed as missing but shares a keyword already present "
                            + "in the resume -- inconsistent reasoning: " + result.reasoning());
        }
    }

    private static List<String> significantTokens(String phrase) {
        return Arrays.stream(phrase.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3 && !STOPWORDS.contains(token))
                .toList();
    }
}
