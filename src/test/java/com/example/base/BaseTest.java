package com.example.base;

import com.microsoft.playwright.*;
import io.qameta.allure.Allure;
import org.testng.ITestResult;
import org.testng.annotations.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public void tearDownMethod(ITestResult result) throws IOException {
        if (result.getStatus() == ITestResult.FAILURE) {
            String testName = result.getMethod().getMethodName();

            Path screenshotPath = Paths.get("target/screenshots/" + testName + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
            Allure.addAttachment(testName + " - screenshot", "image/png", Files.newInputStream(screenshotPath), "png");

            Path tracePath = Paths.get("target/traces/" + testName + "-trace.zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
            Allure.addAttachment(testName + " - trace", "application/zip", Files.newInputStream(tracePath), "zip");
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
