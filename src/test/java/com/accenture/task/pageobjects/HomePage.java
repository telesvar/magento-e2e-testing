package com.accenture.task.pageobjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HomePage extends BasePage {

    @FindBy(css = ".block-promo.home-main")
    private WebElement mainPromoBlock;

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void navigateToHomePage(String baseUrl) {
        logger.info("Navigating to Home Page: {}", baseUrl);
        driver.get(baseUrl);
        try {
            waitForElementToBeVisible(mainPromoBlock);
            logger.info("Home Page loaded successfully (main promo block visible).");
        } catch (Exception e) {
            logger.error("Home page main promo block did not become visible.", e);
        }
    }

    public SignInPage goToSignInFromHome() {
        return getHeader().clickSignIn();
    }

    public CreateAccountPage goToCreateAccountFromHome() {
        return getHeader().clickCreateAccount();
    }
}