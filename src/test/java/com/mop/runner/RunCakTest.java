package com.mop.runner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/Cak.feature",
        glue = {"com.Cakvalidation"},
        monochrome = true,
//        tags = "@cak"
                plugin = {
                "pretty",
                "html:target/cucumber-reports/Cak.html" // fallback
        }
)
public class RunCakTest extends BaseTestRunner {

    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }
}
