package com.mop.runner;


import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/Fobo.feature",
        glue = {"com.Fobo"},
        monochrome = true,
        plugin ={
                "pretty",
                "html:target/cucumber-reports/Fobo.html"
        }
        ,
        tags = "@Fobo"
)


public class FoboRunner extends BaseTestRunner {

    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }
}
