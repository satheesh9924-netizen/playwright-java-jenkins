package com.example.tests;

import com.example.base.BaseTest;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.testng.annotations.Test;
import java.util.regex.Pattern;

public class PlaywrightWebsiteTest extends BaseTest {

    @Test(description = "Verify Playwright homepage title")
    public void testHomepageTitle() {
        page.navigate("https://playwright.dev");
        PlaywrightAssertions.assertThat(page).hasTitle(Pattern.compile("Playwright"));
    }

    @Test(description = "Verify Docs navigation")
    public void testDocsNavigation() {
        page.navigate("https://playwright.dev");
        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK, 
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Get started")).click();
        PlaywrightAssertions.assertThat(page).hasURL(Pattern.compile(".*docs/intro"));
        PlaywrightAssertions.assertThat(
            page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING, 
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(Pattern.compile("Installation", Pattern.CASE_INSENSITIVE)))
        ).isVisible();
    }
}
