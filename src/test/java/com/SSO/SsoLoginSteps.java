package com.SSO;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.Scenario;
import io.cucumber.java.After;

public class SsoLoginSteps {
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private String SSO_USERNAME = System.getenv("SSO_USERNAME");
    private String SSO_PASSWORD = System.getenv("SSO_PASSWORD");
    private Scenario scenario;





    @After
    public void captureFailureScreenshot(Scenario scenario) {

        if (scenario.isFailed() && page != null) {

            String environment = SsoEnvironmentConfig.getEnvironment();

            byte[] screenshot = page.screenshot(
                    new Page.ScreenshotOptions()
                            .setFullPage(true)
            );

            scenario.attach(
                    screenshot,
                    "image/png",
                    "Failure Screenshot - " + environment
            );

            System.out.println(
                    "Failure screenshot captured for environment: "
                            + environment
            );
        }
    }


    @Before
    public void setUp(Scenario scenario) {
        this.scenario = scenario;
    }
    @Given("I open the MOP SSO login page")
    public void iOpenTheMopSsoLoginPage() {

        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
        );

        page = browser.newPage();

        String environment = SsoEnvironmentConfig.getEnvironment();
        String url = SsoEnvironmentConfig.getUrl();

        System.out.println("Running environment: " + environment);
        System.out.println("SSO URL: " + url);

        page.navigate(url);

        System.out.println("Current URL: " + page.url());
    }

    @When("I login using the configured Microsoft account")
    public void iLoginUsingTheConfiguredMicrosoftAccount() {

        System.out.println("Microsoft login page opened.");

        // Enter email
        page.getByPlaceholder("Email, phone, or Skype")
                .fill(SSO_USERNAME);

        // Click Next
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Next")
        ).click();

        System.out.println("Email entered and Next clicked.");

        // Wait for password field
        page.getByPlaceholder("Password")
                .waitFor();

        // Enter password
        page.getByPlaceholder("Password")
                .fill(SSO_PASSWORD);

        System.out.println("Password entered.");

        // Click Sign in
        // Click Sign in
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Sign in")
        ).click();

        // Wait for "Stay signed in?" page
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Yes")
        ).waitFor();

// Click Yes
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Yes")
        ).click();

        System.out.println("Clicked 'Yes' on Stay signed in.");

        page.waitForTimeout(5000);

        System.out.println(
                "URL after Stay signed in: " + page.url()
        );
    }



    @Then("I should be successfully logged into the MOP dashboard")
    public void iShouldBeSuccessfullyLoggedIntoTheMopDashboard() {


        page.waitForURL("**/client/index.html#/dashboard");

        String actualUrl = page.url();

        System.out.println("Actual dashboard URL: " + actualUrl);

        if (!actualUrl.contains("/client/index.html#/dashboard")) {
            throw new AssertionError(
                    "Login failed. Actual URL: " + actualUrl
            );
        }

        System.out.println(
                "SSO login successful. MOP dashboard is displayed."
        );

        String environment = SsoEnvironmentConfig.getEnvironment();

        System.out.println(
                "SSO login successful for environment: " + environment
        );
        // Take screenshot
        byte[] screenshot = page.screenshot(
                new Page.ScreenshotOptions()
                        .setFullPage(true)
        );



        // Attach screenshot to Cucumber report
        scenario.attach(
                screenshot,
                "image/png",
                "MOP Dashboard - " + environment
        );

    }
}

