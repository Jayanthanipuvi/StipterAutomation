package com.qa.factory;
import com.qa.util.PropertiesFileReader;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.time.Duration;


public class GlobalDriverConfig {
    public WebDriver driver;
    public WebDriverWait wait;
    public static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(GlobalDriverConfig.class);


    public GlobalDriverConfig() {
        String browser = PropertiesFileReader.getProperty("browser");
        String waitDurationInSec = PropertiesFileReader.getProperty("wait.duration");
        logger.info("Browser : " + browser);
        DriverFactory driverFactory = new DriverFactory();
        driver = driverFactory.init_driver(browser);
        driver = DriverFactory.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(Long.parseLong(waitDurationInSec)));

    }

}