package com.accenture.task.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class CreateAccountPage extends BasePage {

    private final By firstNameErrorLocator = By.id("firstname-error");
    private final By lastNameErrorLocator = By.id("lastname-error");
    private final By emailErrorLocator = By.id("email_address-error");
    private final By passwordErrorLocator = By.id("password-error");
    private final By confirmPasswordErrorLocator = By.id("password-confirmation-error");

    @FindBy(id = "firstname")
    private WebElement firstNameInput;
    @FindBy(id = "lastname")
    private WebElement lastNameInput;
    @FindBy(id = "email_address")
    private WebElement emailInput;
    @FindBy(id = "password")
    private WebElement passwordInput;
    @FindBy(id = "password-confirmation")
    private WebElement confirmPasswordInput;

    @FindBy(css = "button.action.submit.primary[title='Create an Account']")
    private WebElement createAccountButton;

    @FindBy(css = ".page-title span.base")
    private WebElement pageTitle;
    @FindBy(css = "div[data-ui-id='message-error']")
    private WebElement generalErrorMessage;
    
    public CreateAccountPage(WebDriver driver) {
        super(driver);
        waitForElementToBeVisible(pageTitle);
    }

    // --- Actions ---

    public void enterFirstName(String firstName) {
        sendKeysToElement(firstNameInput, firstName);
    }

    public void enterLastName(String lastName) {
        sendKeysToElement(lastNameInput, lastName);
    }

    public void enterEmail(String email) {
        sendKeysToElement(emailInput, email);
    }

    public void enterPassword(String password) {
        sendKeysToElement(passwordInput, password);
    }

    public void enterConfirmPassword(String password) {
        sendKeysToElement(confirmPasswordInput, password);
    }

    public void clickCreateAccountButton() {
        logger.info("Clicking Create an Account button");
        clickElement(createAccountButton);
    }

    public AccountPage registerUser(String firstName, String lastName, String email, String password) {
        logger.info("Attempting to register user: {} {} ({})", firstName, lastName, email);
        enterFirstName(firstName);
        enterLastName(lastName);
        enterEmail(email);
        enterPassword(password);
        enterConfirmPassword(password);
        clickCreateAccountButton();
        return new AccountPage(driver);
    }

    // --- Getters for Verification ---

    public String getFieldErrorText(String fieldName) {
        By locator = By.id(fieldName + "-error");
        try {
            WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            String errorText = errorElement.getText();
            logger.debug("Found error for field '{}': {}", fieldName, errorText);
            return errorText;
        } catch (Exception e) {
            logger.warn("Error element not found or not visible for field ID: {}-error", fieldName);
            return "";
        }
    }

    public String getGeneralErrorText() {
        try {
            // General errors might take a moment to appear after submission
            wait.withTimeout(Duration.ofSeconds(5)).until(ExpectedConditions.visibilityOf(generalErrorMessage));
            String errorText = getTextFromElement(generalErrorMessage);
            logger.debug("Found general error message: {}", errorText);
            return errorText;
        } catch (Exception e) { // Catch timeout or no such element
            logger.warn("General error message element not found or timed out.");
            return "";
        }
    }

    public String getPageTitle() {
        waitForElementToBeVisible(pageTitle);
        return getTextFromElement(pageTitle);
    }
}