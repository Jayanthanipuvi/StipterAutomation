package com.mop.runner;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/Product",
        glue = {"com.Product"},
        monochrome = true,
        plugin ={
                "pretty",
                "html:target/cucumber-reports/Product.html"
        },
        tags = "@Product"
)
public class ProductRunner extends BaseTestRunner{


    @Test
    public void runCucumber() {
        // This method forces Gradle to execute TestNG
    }
}
