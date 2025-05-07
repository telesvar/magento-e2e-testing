package com.accenture.task.pageobjects;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SignInPage extends BasePage {

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "pass")
    private WebElement passwordInput;

    @FindBy(css = ".action.login.primary#send2")
    private WebElement signInButton;

    @FindBy(css = ".action.create.primary")
    private WebElement createAccountButton;

    @FindBy(css = ".action.remind")
    private WebElement forgotPasswordLink;

    @FindBy(css = ".page-title span.base")
    private WebElement pageTitle;

    @FindBy(css = "div[data-ui-id='message-error']")
    private WebElement loginErrorMessage;

    // REMOVED: @FindBy for accountPageWelcomeMessage - verification moved

    public SignInPage(WebDriver driver) {
        super(driver);
        waitForElementToBeVisible(emailInput);
    }

    public void enterEmail(String email) {
        sendKeysToElement(emailInput, email);
    }

    public void enterPassword(String password) {
        sendKeysToElement(passwordInput, password);
    }

    public void clickSignInButton() {
        logger.info("Clicking Sign In button");
        clickElement(signInButton);
        // Let the caller handle waiting for the next page or error message
    }

    /**
     * Enters credentials and clicks Sign In.
     * Does NOT verify success. The caller should instantiate the expected next page (AccountPage)
     * and rely on its constructor/methods to verify the navigation.
     *
     * @param email    User's email.
     * @param password User's password.
     */
    public void attemptLogin(String email, String password) {
        logger.info("Attempting login action for user: {}", email);
        enterEmail(email);
        enterPassword(password);
        clickSignInButton();
    }

    // Keep attemptLoginExpectingError as is, or simplify if preferred

    public SignInPage attemptLoginExpectingError(String email, String password) {
        logger.info("Attempting login expecting error for user: {}", email);
        attemptLogin(email, password); // Use the simplified action method
        // Wait specifically for the error message on *this* page
        try {
            waitForElementToBeVisible(loginErrorMessage);
        } catch (Exception e) {
            logger.warn("Expected login error message did not appear.", e);
        }
        return this;
    }


    public CreateAccountPage clickCreateAccount() {
        logger.info("Clicking Create Account button from Sign In page");
        clickElement(createAccountButton);
        return new CreateAccountPage(driver);
    }

    public String getPageTitle() {
        waitForElementToBeVisible(pageTitle);
        return getTextFromElement(pageTitle);
    }

    public String getLoginErrorMessage() {
        try {
            // Wait for the error message specifically if checking for failure
            waitForElementToBeVisible(loginErrorMessage);
            return getTextFromElement(loginErrorMessage);
        } catch (NoSuchElementException e) {
            logger.warn("Login error message element not found.");
            return "";
        } catch (Exception e) {
            logger.error("Error waiting for login error message", e);
            return "";
        }
    }
}