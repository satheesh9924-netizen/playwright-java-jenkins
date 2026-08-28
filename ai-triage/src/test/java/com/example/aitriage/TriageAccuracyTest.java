package com.example.aitriage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Golden-set regression suite for the triage bot itself: each fixture is a synthetic failure
 * with an obvious human-judged category. Re-run whenever SYSTEM_PROMPT changes to catch
 * classification regressions before they reach real CI failures.
 */
public class TriageAccuracyTest {

    private record GoldenFailureCase(
            String className, String testName, String errorMessage, String stackTrace, String expectedCategory) {
    }

    @DataProvider(name = "goldenFailures")
    public Object[][] goldenFailures() {
        return new Object[][]{
                {"flaky_timeout.json"},
                {"regression_selector.json"},
                {"environment_docker.json"},
        };
    }

    @Test(dataProvider = "goldenFailures")
    public void classifiesFailureIntoExpectedCategory(String fixtureFile) throws Exception {
        GoldenFailureCase goldenCase = readFixture(fixtureFile);
        TestFailure failure = new TestFailure(
                goldenCase.className(), goldenCase.testName(), goldenCase.errorMessage(), goldenCase.stackTrace());

        FailureTriage triage = new FailureTriage(LlmClientFactory.create());
        TriageResult result = triage.triage(failure);

        assertTrue(TriageResult.VALID_CATEGORIES.contains(result.category()),
                "Unexpected category: " + result.category());
        assertEquals(result.category(), goldenCase.expectedCategory(),
                "[" + fixtureFile + "] Summary was: " + result.summary());
    }

    private GoldenFailureCase readFixture(String fileName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/golden/" + fileName)) {
            return mapper.readValue(in, GoldenFailureCase.class);
        }
    }
}
