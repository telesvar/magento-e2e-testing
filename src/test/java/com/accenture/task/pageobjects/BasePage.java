package com.accenture.task.pageobjects;

import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public abstract class BasePage {
    protected static final Logger logger = LoggerFactory.getLogger(BasePage.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
        PageFactory.initElements(driver, this);
        logger.debug("Initialized Page: {}", this.getClass().getSimpleName());
    }

    public HeaderPage getHeader() {
        return new HeaderPage(driver);
    }

    protected void waitForElementToBeVisible(WebElement element) {
        try {
            wait.until(ExpectedConditions.visibilityOf(element));
            logger.debug("Element is visible: {}", getShortElementDescription(element));
        } catch (Exception e) {
            logger.error("Timeout waiting for element visibility: {}", getShortElementDescription(element), e);
            throw e;
        }
    }

    protected void waitForElementToBeClickable(WebElement element) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element));
            logger.debug("Element is clickable: {}", getShortElementDescription(element));
        } catch (Exception e) {
            logger.error("Timeout waiting for element to be clickable: {}", getShortElementDescription(element), e);
            throw e;
        }
    }

    /**
     * Clicks an element, attempting standard click first, then JavaScript click as fallback.
     * Includes retry for StaleElementReferenceException.
     *
     * @param element The WebElement to click.
     */
    protected void clickElement(WebElement element) {
        int attempts = 0;
        while (attempts < 2) {
            try {
                waitForElementToBeClickable(element);
                String elementDesc = getShortElementDescription(element);
                logger.info("Attempting standard click on element: {}", elementDesc);
                element.click();
                logger.debug("Standard click successful for: {}", elementDesc);
                return; // Success
            } catch (StaleElementReferenceException e) {
                attempts++;
                logger.warn("StaleElementReferenceException caught (attempt {}). Retrying click for element: {}", attempts, getShortElementDescription(element));
                PageFactory.initElements(driver, this); // Refresh elements
                if (attempts >= 2) {
                    logger.error("Failed to click element after retrying due to StaleElementReferenceException: {}", getShortElementDescription(element), e);
                    throw e;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (ElementClickInterceptedException ecie) {
                logger.warn("ElementClickInterceptedException caught for element: {}. Trying JavaScript click.", getShortElementDescription(element));
                try {
                    clickElementWithJavaScript(element);
                    return; // Success with JS click
                } catch (Exception jsException) {
                    logger.error("JavaScript click also failed for element: {}", getShortElementDescription(element), jsException);
                    throw ecie; // Re-throw original interception exception if JS fails
                }
            } catch (Exception e) {
                logger.error("Failed to click element with standard click: {}", getShortElementDescription(element), e);
                throw e; // Throw other exceptions immediately
            }
        }
    }

    /**
     * Uses JavaScriptExecutor to click an element. Useful for intercepted elements.
     *
     * @param element The WebElement to click.
     */
    protected void clickElementWithJavaScript(WebElement element) {
        String elementDesc = getShortElementDescription(element);
        logger.info("Attempting JavaScript click on element: {}", elementDesc);
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            logger.debug("JavaScript click successful for: {}", elementDesc);
        } catch (Exception e) {
            logger.error("JavaScript click failed for element: {}", elementDesc, e);
            throw e; // Re-throw exception
        }
    }


    protected void sendKeysToElement(WebElement element, String text) {
        try {
            waitForElementToBeVisible(element);
            logger.info("Sending keys '{}' to element: {}", text, getShortElementDescription(element));
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            logger.error("Failed to send keys '{}' to element: {}", text, getShortElementDescription(element), e);
            throw e;
        }
    }

    protected String getTextFromElement(WebElement element) {
        try {
            waitForElementToBeVisible(element);
            String text = element.getText();
            logger.debug("Retrieved text '{}' from element: {}", text, getShortElementDescription(element));
            return text;
        } catch (Exception e) {
            logger.error("Failed to get text from element: {}", getShortElementDescription(element), e);
            // Return empty string instead of throwing to potentially allow checks for absence
            return "";
        }
    }

    private String getShortElementDescription(WebElement element) {
        if (element == null) return "null";
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) return "id=" + id;
        String name = element.getAttribute("name");
        if (name != null && !name.isEmpty()) return "name=" + name;
        String tagName = element.getTagName();
        String className = element.getAttribute("class");
        if (className != null && !className.isEmpty()) return tagName + ".class='" + className.split(" ")[0] + "'";
        String elementString = element.toString();
        return elementString.substring(0, Math.min(elementString.length(), 80)) + "...";
    }
}