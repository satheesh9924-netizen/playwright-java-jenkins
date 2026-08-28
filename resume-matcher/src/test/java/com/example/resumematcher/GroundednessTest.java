package com.example.resumematcher;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.testng.Assert.assertTrue;

/**
 * Checks that the model's claims are grounded in the source documents rather than invented.
 * A "matched" skill must share at least one meaningful keyword with the resume (lenient --
 * catches skills sharing zero real overlap, i.e. outright fabrications). A "missing" skill
 * must NOT have ALL of its meaningful keywords present (strict -- a single generic shared word
 * like "testing" isn't enough to call a compound skill phrase "already present"; the whole
 * claimed capability has to actually be there for the claim to be inconsistent).
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
            boolean fullyPresent = !tokens.isEmpty() && tokens.stream().allMatch(resumeLower::contains);
            assertTrue(!fullyPresent,
                    "Skill \"" + skill + "\" was listed as missing but is fully present in the resume -- "
                            + "inconsistent reasoning: " + result.reasoning());
        }
    }

    private static List<String> significantTokens(String phrase) {
        return Arrays.stream(phrase.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3 && !STOPWORDS.contains(token))
                .toList();
    }
}
