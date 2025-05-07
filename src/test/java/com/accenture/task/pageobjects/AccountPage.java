package com.accenture.task.pageobjects;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class AccountPage extends BasePage {

    @FindBy(css = ".page-title span.base")
    private WebElement pageTitle; // Expected: "My Account"

    @FindBy(css = ".box-information .box-content")
    private WebElement contactInfoBoxContent;

    @FindBy(css = ".box-information .box-content p")
    private WebElement contactInfoParagraph;

    @FindBy(css = "div[data-ui-id='message-success']")
    private WebElement successMessage;

    // Add locator for login page element to detect incorrect navigation
    @FindBy(id = "email") // Check if login email input is present
    private WebElement loginEmailInput;


    public AccountPage(WebDriver driver) {
        super(driver);
        String currentPageUrl = "N/A"; // Initialize for logging
        try {
            currentPageUrl = driver.getCurrentUrl(); // Get URL for context in case of error
            // Wait for elements that *must* be present on the Account Page
            wait.until(ExpectedConditions.visibilityOf(pageTitle));
            wait.until(ExpectedConditions.visibilityOf(contactInfoBoxContent));

            // Explicit check: Are we *really* on the account page?
            String titleText = pageTitle.getText();
            if (!titleText.equalsIgnoreCase("My Account")) {
                throw new IllegalStateException("Expected Account Page title 'My Account' but found '" + titleText + "'. Current URL: " + currentPageUrl);
            }

            // Double-check we are NOT on the login page (paranoid check)
            try {
                if (loginEmailInput.isDisplayed()) {
                    throw new IllegalStateException("Attempted to initialize AccountPage, but found Login Page elements (email input visible). Login likely failed. Current URL: " + currentPageUrl);
                }
            } catch (NoSuchElementException e) {
                // This is the expected case - the login input should NOT be found.
                logger.info("Account Page loaded successfully (title verified). Current URL: {}", currentPageUrl);
            }

        } catch (TimeoutException e) {
            logger.error("Account page did not load correctly within timeout (title or contact info not found). Current URL: {}", currentPageUrl, e);
            // Check if we landed on the login page instead
            try {
                SignInPage signInPage = new SignInPage(driver); // Try to init SignInPage
                if (signInPage.getPageTitle().equalsIgnoreCase("Customer Login")) {
                    String loginError = signInPage.getLoginErrorMessage();
                    throw new IllegalStateException("Login failed, ended up on Login Page instead of Account Page. Login error: '" + loginError + "'. Current URL: " + currentPageUrl, e);
                }
            } catch (Exception checkException) {
                logger.error("Also failed to check if it's the Login Page after Account Page timeout.", checkException);
            }
            throw new IllegalStateException("Account Page elements not found within timeout. Current URL: " + currentPageUrl, e); // Make failure clearer
        } catch (Exception e) {
            logger.error("An unexpected error occurred during AccountPage initialization. Current URL: {}", currentPageUrl, e);
            throw e;
        }
    }

    public String getPageTitle() {
        return getTextFromElement(pageTitle);
    }

    public String getContactInfoText() {
        waitForElementToBeVisible(contactInfoParagraph);
        return getTextFromElement(contactInfoParagraph);
    }

    public String getSuccessMessage() {
        try {
            wait.withTimeout(Duration.ofSeconds(5)).until(ExpectedConditions.visibilityOf(successMessage));
            return getTextFromElement(successMessage);
        } catch (Exception e) {
            logger.warn("Success message element not found or timed out.");
            return "";
        }
    }

    public boolean isUserLoggedInOnAccountPage() {
        // Rely on header check which is accessible via getHeader()
        return getHeader().isUserLoggedIn();
    }
}