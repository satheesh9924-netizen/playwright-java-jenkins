package com.example.resumematcher;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Golden-set regression suite: each case has a human-judged expected score RANGE (not an
 * exact value) and expected verdict. Re-run this whenever the prompt changes to catch
 * quality regressions -- this is the LLM-feature equivalent of a unit test suite.
 */
public class AccuracyGoldenSetTest {

    @DataProvider(name = "goldenCases")
    public Object[][] goldenCases() {
        return new Object[][]{
                {"case1_strong_fit"},
                {"case2_no_fit"},
                {"case3_partial_fit"},
        };
    }

    @Test(dataProvider = "goldenCases")
    public void matchScoreFallsWithinExpectedRange(String caseName) {
        String resume = TestSupport.readGoldenFile(caseName, "resume.txt");
        String jd = TestSupport.readGoldenFile(caseName, "jd.txt");
        TestSupport.ExpectedRange expected = TestSupport.readExpected(caseName);

        MatchResult result = TestSupport.MATCHER.match(resume, jd);

        assertTrue(result.matchScore() >= expected.minScore() && result.matchScore() <= expected.maxScore(),
                "[" + caseName + "] matchScore " + result.matchScore()
                        + " outside expected range [" + expected.minScore() + "," + expected.maxScore() + "]"
                        + ". Reasoning was: " + result.reasoning());

        assertEquals(result.verdict(), expected.verdict(),
                "[" + caseName + "] unexpected verdict. Reasoning was: " + result.reasoning());
    }
}
