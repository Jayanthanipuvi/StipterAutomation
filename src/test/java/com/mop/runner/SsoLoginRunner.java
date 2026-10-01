package com.mop.runner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/SsoLogin",
        glue = {"com.SSO"},
        monochrome = true,
        plugin = {
                "pretty",
                "html:target/cucumber-reports/SSO.html"
        },
        tags = "@SSO"
)
public class SsoLoginRunner extends BaseTestRunner {

    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }
}