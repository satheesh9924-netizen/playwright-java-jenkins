package com.example.base;

import com.microsoft.playwright.*;
import org.testng.ITestResult;
import org.testng.annotations.*;
import java.nio.file.Paths;

public class BaseTest {
    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeClass
    public void setUpClass() {
        playwright = Playwright.create();
        boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(isHeadless));
    }

    @BeforeMethod
    public void setUpMethod() {
        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        page = context.newPage();
    }

    @AfterMethod
    public void tearDownMethod(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            String testName = result.getMethod().getMethodName();
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("target/screenshots/" + testName + ".png")));
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/traces/" + testName + "-trace.zip")));
        } else {
            context.tracing().stop();
        }
        if (context != null) context.close();
    }

    @AfterClass
    public void tearDownClass() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
