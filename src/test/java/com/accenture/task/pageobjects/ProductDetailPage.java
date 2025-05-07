package com.accenture.task.pageobjects;

import com.accenture.task.utils.TestUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;

public class ProductDetailPage extends BasePage {

    private final By sizeOptionLocator = By.cssSelector(".swatch-option.text");
    private final By colorOptionLocator = By.cssSelector(".swatch-option.color");
    @FindBy(css = ".page-title-wrapper .base[itemprop='name']")
    private WebElement productNameTitle;
    @FindBy(css = ".product-info-main .price-box .price")
    private WebElement productPrice;
    @FindBy(id = "qty")
    private WebElement quantityInput;
    @FindBy(id = "product-addtocart-button")
    private WebElement addToCartButton;
    @FindBy(css = ".stock.available span")
    private WebElement stockAvailableSpan;
    @FindBy(css = ".stock.unavailable span")
    private WebElement stockUnavailableSpan;
    @FindBy(css = "div[data-ui-id='message-success']")
    private WebElement successMessage;
    @FindBy(css = "div[data-ui-id='message-error']")
    private WebElement errorMessageTop;
    @FindBy(css = ".message.error") // General error includes qty not available
    private WebElement errorMessageGeneral;
    @FindBy(css = ".swatch-attribute.size")
    private WebElement sizeSwatchContainer;
    @FindBy(css = ".swatch-attribute.color")
    private WebElement colorSwatchContainer;


    public ProductDetailPage(WebDriver driver) {
        super(driver);
        waitForElementToBeVisible(productNameTitle);
        waitForElementToBeVisible(productPrice);
        logger.info("Product Detail Page loaded for: {}", getProductName());
    }

    public String getProductName() {
        return getTextFromElement(productNameTitle);
    }

    public double getProductPrice() {
        String priceText = getTextFromElement(productPrice);
        return TestUtils.extractPrice(priceText);
    }

    public void enterQuantity(String qty) {
        sendKeysToElement(quantityInput, String.valueOf(qty));
    }

    public boolean hasSizeOptions() {
        try {
            // Use findElements and check size to avoid exception
            return !driver.findElements(By.cssSelector(".swatch-attribute.size")).isEmpty();
        } catch (Exception e) { // Catch potential errors during find
            logger.warn("Error checking for size options", e);
            return false;
        }
    }

    public boolean hasColorOptions() {
        try {
            // Use findElements and check size
            return !driver.findElements(By.cssSelector(".swatch-attribute.color")).isEmpty();
        } catch (Exception e) {
            logger.warn("Error checking for color options", e);
            return false;
        }
    }

    public void selectFirstAvailableSize() {
        if (!hasSizeOptions()) {
            logger.info("No size options present for this product.");
            return;
        }
        logger.info("Selecting first available size...");
        List<WebElement> sizeOptions = driver.findElements(sizeOptionLocator); // Re-find within the method
        for (WebElement option : sizeOptions) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(option));
                clickElement(option);
                logger.info("Selected size: {}", option.getAttribute("option-label"));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            } catch (Exception e) {
                logger.trace("Size option '{}' not clickable, trying next.", option.getAttribute("option-label"));
            }
        }
        throw new NoSuchElementException("No available/clickable size option found.");
    }

    public void selectFirstAvailableColor() {
        if (!hasColorOptions()) {
            logger.info("No color options present for this product.");
            return;
        }
        logger.info("Selecting first available color...");
        List<WebElement> colorOptions = driver.findElements(colorOptionLocator); // Re-find within the method
        for (WebElement option : colorOptions) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(option));
                clickElement(option);
                logger.info("Selected color: {}", option.getAttribute("option-label"));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            } catch (Exception e) {
                logger.trace("Color option '{}' not clickable, trying next.", option.getAttribute("option-label"));
            }
        }
        throw new NoSuchElementException("No available/clickable color option found.");
    }


    public void clickAddToCart() {
        logger.info("Clicking Add to Cart button for product: {}", getProductName());
        waitForElementToBeClickable(addToCartButton);
        clickElement(addToCartButton);
        logger.debug("Add to cart button clicked.");
    }


    public boolean isProductAvailable() {
        try {
            waitForElementToBeVisible(stockAvailableSpan);
            boolean available = "In stock".equalsIgnoreCase(getTextFromElement(stockAvailableSpan));
            logger.debug("Product availability (available span): {}", available);
            return available;
        } catch (NoSuchElementException e) {
            try {
                // Check visibility directly
                if (stockUnavailableSpan.isDisplayed()) {
                    logger.debug("Product availability (unavailable span displayed): false");
                    return false;
                }
                logger.warn("'In stock' span not found, and 'Out of stock' span also not found or not visible.");
                return false; // Default to unavailable
            } catch (NoSuchElementException ex) {
                logger.warn("Could not determine stock status - neither 'In stock' nor 'Out of stock' span found.");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking product availability", e);
            return false; // Default to unavailable on error
        }
    }

    public String getSuccessMessage() {
        try {
            wait.withTimeout(Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOf(successMessage));
            return getTextFromElement(successMessage);
        } catch (Exception e) {
            logger.warn("Success message element not found or timed out.");
            return "";
        }
    }

    /**
     * Gets the error message text (e.g., "The requested qty is not available").
     * Checks multiple potential error locations after waiting for one to appear.
     *
     * @return The error message, or empty string if not found.
     */
    public String getErrorMessage() {
        try {
            // Wait for *either* potential error message element to be visible
            wait.withTimeout(Duration.ofSeconds(5)).until(ExpectedConditions.or(ExpectedConditions.visibilityOf(errorMessageTop), ExpectedConditions.visibilityOf(errorMessageGeneral)));

            // Now check which one is actually displayed and get its text
            if (errorMessageTop.isDisplayed()) {
                String errorText = getTextFromElement(errorMessageTop);
                logger.info("Found top error message: {}", errorText);
                return errorText;
            } else if (errorMessageGeneral.isDisplayed()) {
                String errorText = getTextFromElement(errorMessageGeneral);
                logger.info("Found general error message: {}", errorText);
                return errorText;
            } else {
                // This case should be rare if the wait.until(or(...)) succeeded
                logger.warn("Waited for error message, but neither known error element is displayed now.");
                return "";
            }
        } catch (TimeoutException e) {
            logger.warn("No known error message element appeared on product page within timeout.");
            return "";
        } catch (NoSuchElementException e) {
            // This might happen if the elements disappear *after* the wait condition was met but before isDisplayed() check
            logger.warn("Error message element became stale or disappeared after initial wait.");
            return "";
        } catch (Exception e) {
            logger.error("Unexpected error while getting error message.", e);
            return "";
        }
    }
}