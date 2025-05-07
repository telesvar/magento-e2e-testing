package com.accenture.task.tests;

import com.accenture.task.pageobjects.HomePage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public abstract class BaseTest {

    protected static final String BASE_URL = "https://magento.softwaretestingboard.com/";
    protected static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected static final Duration TIMEOUT = Duration.ofSeconds(20);
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected HomePage homePage;

    @BeforeAll
    static void setupClass() {
        logger.info("Setting up WebDriverManager for Chrome...");
        try {
            WebDriverManager.chromedriver().setup();
            logger.info("WebDriverManager setup complete.");
        } catch (Exception e) {
            logger.error("WebDriverManager setup failed!", e);
            throw e;
        }
    }

    private static ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        return options;
    }

    @BeforeEach
    void setupTest() {
        logger.info("Initializing WebDriver...");
        try {
            ChromeOptions options = getChromeOptions();

            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, TIMEOUT);

            driver.manage().window().maximize();

            homePage = new HomePage(driver);
            homePage.navigateToHomePage(BASE_URL);
            logger.info("WebDriver initialized and navigated to base URL.");
        } catch (Exception e) {
            logger.error("WebDriver initialization failed!", e);
            if (driver != null) {
                driver.quit();
            }
            throw e;
        }
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            logger.info("Quitting WebDriver...");
            try {
                driver.quit();
                logger.info("WebDriver quit successfully.");
            } catch (Exception e) {
                logger.error("Error quitting WebDriver.", e);
            }
        }
    }
}