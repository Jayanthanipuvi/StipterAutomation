package com.qa.factory;

import com.qa.util.PropertiesFileReader;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.wdm.WebDriverManager;


public class DriverFactory {

    public static ThreadLocal<WebDriver> tlDriver = new ThreadLocal<>();
    public static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DriverFactory.class);

    /**
     * Initialize the ThreadLocal driver based on the given browser.
     */
    public WebDriver init_driver(String browser) {
        boolean isHeadless = Boolean.parseBoolean(PropertiesFileReader.getProperty("headless"));
        logger.info("Launching browser: " + browser + " | Headless mode: " + isHeadless);

        switch (browser.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                if (isHeadless) {
                    chromeOptions.addArguments("--headless");
                    chromeOptions.addArguments("--disable-gpu");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.addArguments("--window-size=1920,1080");
                }
                tlDriver.set(new ChromeDriver(chromeOptions));
                break;

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (isHeadless) {
                    firefoxOptions.addArguments("--headless");
                }
                tlDriver.set(new FirefoxDriver(firefoxOptions));
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                if (isHeadless) {
                    edgeOptions.addArguments("headless");
                    edgeOptions.addArguments("disable-gpu");
                }
                tlDriver.set(new EdgeDriver(edgeOptions));
                break;

            case "safari":
                WebDriverManager.safaridriver().setup();
                tlDriver.set(new SafariDriver());
                break;

            case "ie":
                WebDriverManager.iedriver().setup();
                tlDriver.set(new InternetExplorerDriver());
                break;

            default:
                logger.error("❌ Invalid browser value: " + browser);
                throw new RuntimeException("Please provide a valid browser name in config.properties");
        }

        getDriver().manage().deleteAllCookies();
        getDriver().manage().window().maximize();
        return getDriver();
    }

    /**
     * Get the ThreadLocal driver instance.
     */
    public static synchronized WebDriver getDriver() {
        return tlDriver.get();
    }
}
