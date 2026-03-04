package com.mop.runner;

import com.stipter.util.ReportUtil;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import org.testng.annotations.BeforeClass;


public abstract class BaseTestRunner extends AbstractTestNGCucumberTests {

    @BeforeClass(alwaysRun = true)
    public void setupReport() {
        ReportUtil.setCucumberReport();
    }
}
