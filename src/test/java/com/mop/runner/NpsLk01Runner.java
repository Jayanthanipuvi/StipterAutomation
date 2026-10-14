package com.mop.runner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/BRP",
        glue = {"com.BRP"},
        monochrome = true,
        plugin = {
                "html:target/cucumber-reports/NPSLK01.html"
        },
        tags = "@NPSLK01"
)
public class NpsLk01Runner extends BaseTestRunner {

    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }
}
