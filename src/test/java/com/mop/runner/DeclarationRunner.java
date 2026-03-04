package com.mop.runner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/Declaration.feature",
        glue = {"com.Declarationvalidation"},
        monochrome = true,
plugin ={
        "pretty",
        "html:target/cucumber-reports/Declaration.html"
        },
        tags = "@declaration"
)
public class DeclarationRunner extends BaseTestRunner {

    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }
}
