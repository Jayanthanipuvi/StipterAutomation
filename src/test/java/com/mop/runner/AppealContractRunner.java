package com.mop.runner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;


@CucumberOptions(
        features = "classpath:features/ApealContract.feature",
        glue = {"com.AppealContract"},
        monochrome = true,
        plugin ={
                "pretty",
                "html:target/cucumber-reports/Fobo.html"
        }
        ,
        tags = "@AppealContract"
)

public class AppealContractRunner extends BaseTestRunner {

    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }





}
