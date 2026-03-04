package com.mop.runner;

import com.stipter.util.ReportUtil;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "classpath:features/SepaJob",
        glue = {"com.SepajobValidation"},
        monochrome = true,
        plugin = {
                "pretty",
                "html:target/cucumber-reports/Sepa.html" // fallback
        },
        tags = "@sepajob"
)
public class SepajobTest extends BaseTestRunner {



    @Test
    public void runCucumber() {
        // required by your framework
    }
}
