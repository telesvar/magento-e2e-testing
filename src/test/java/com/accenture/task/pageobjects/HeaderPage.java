package com.accenture.task.pageobjects;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderPage extends BasePage {

    // Locator for the empty class on the parent span
    private final By cartCounterEmptyClassLocator = By.cssSelector("a.action.showcart .counter.qty.empty");

    @FindBy(linkText = "Sign In")
    private WebElement signInLink;
    @FindBy(linkText = "Create an Account")
    private WebElement createAccountLink;

    @FindBy(css = ".greet.welcome .logged-in")
    private WebElement welcomeMessageLoggedIn;
    @FindBy(css = ".greet.welcome .not-logged-in")
    private WebElement notLoggedInSpan;

    @FindBy(css = "a.action.showcart")
    private WebElement cartIcon;
    @FindBy(css = "a.action.showcart .counter.qty")
    private WebElement cartCounterParentSpan;
    @FindBy(css = ".counter.qty .counter-number")
    private WebElement cartCounterNumber;
    @FindBy(css = ".minicart-wrapper")
    private WebElement minicartWrapper;

    @FindBy(css = "#top-cart-btn-checkout")
    private WebElement proceedToCheckoutButton;

    @FindBy(css = ".subtotal .price-wrapper .price")
    private WebElement cartSubtotalPrice;

    @FindBy(css = "#mini-cart .product-item-name a")
    private List<WebElement> cartItemNames;

    @FindBy(css = "#mini-cart .minicart-price .price")
    private List<WebElement> cartItemPrices;

    @FindBy(css = "#mini-cart .details-qty .cart-item-qty")
    private List<WebElement> cartItemQuantities;

    @FindBy(id = "search")
    private WebElement searchInput;

    @FindBy(css = ".action.search[type='submit']")
    private WebElement searchButton;

    @FindBy(css = ".customer-welcome button.switch")
    private WebElement customerWelcomeToggle;

    @FindBy(linkText = "My Account")
    private WebElement myAccountLink;

    @FindBy(linkText = "My Wish List")
    private WebElement myWishListLink;

    @FindBy(linkText = "Sign Out")
    private WebElement signOutLink;

    public HeaderPage(WebDriver driver) {
        super(driver);
    }

    public SignInPage clickSignIn() {
        logger.info("Clicking Sign In link");
        clickElement(signInLink);
        return new SignInPage(driver);
    }

    public CreateAccountPage clickCreateAccount() {
        logger.info("Clicking Create an Account link");
        clickElement(createAccountLink);
        return new CreateAccountPage(driver);
    }

    public void expandCart() {
        logger.info("Expanding cart");
        waitForElementToBeClickable(cartIcon);
        clickElement(cartIcon);
        try {
            wait.until(ExpectedConditions.or(ExpectedConditions.visibilityOf(proceedToCheckoutButton), ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#mini-cart .minicart-items"))));
            logger.info("Cart expanded (checkout button or items visible)");
        } catch (Exception e) {
            try {
                wait.withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".minicart-items-wrapper .subtitle.empty")));
                logger.info("Cart expanded (empty message visible).");
            } catch (Exception ex) {
                logger.error("Failed to confirm cart expansion.", ex);
            }
        }
    }

    public ProductListPage searchFor(String term) {
        logger.info("Searching for term: '{}'", term);
        sendKeysToElement(searchInput, term);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        waitForElementToBeClickable(searchButton);
        clickElement(searchButton);
        logger.info("Search submitted for '{}'", term);
        return new ProductListPage(driver);
    }

    public SignInPage clickSignOut() {
        logger.info("Attempting to sign out");
        if (!isUserLoggedIn()) {
            logger.warn("Sign out called but user doesn't appear to be logged in.");
            return new SignInPage(driver);
        }
        waitForElementToBeClickable(customerWelcomeToggle);
        clickElement(customerWelcomeToggle);
        waitForElementToBeClickable(signOutLink);
        clickElement(signOutLink);
        waitForElementToBeVisible(signInLink);
        logger.info("Sign out successful");
        return new SignInPage(driver);
    }

    public String getWelcomeMessage() {
        try {
            waitForElementToBeVisible(welcomeMessageLoggedIn);
            return getTextFromElement(welcomeMessageLoggedIn);
        } catch (NoSuchElementException e) {
            logger.warn("Welcome message (logged-in) not found.");
            return "";
        }
    }

    public boolean isUserLoggedIn() {
        try {
            return welcomeMessageLoggedIn.isDisplayed() || customerWelcomeToggle.isDisplayed();
        } catch (NoSuchElementException e) {
            try {
                return !notLoggedInSpan.isDisplayed();
            } catch (NoSuchElementException innerE) {
                logger.warn("Could not reliably determine login status - relevant elements not found.");
                return false;
            }
        }
    }

    public int getCartCount() {
        try {
            if (!driver.findElements(cartCounterEmptyClassLocator).isEmpty()) {
                logger.info("Cart counter has 'empty' class, returning 0.");
                return 0;
            }
            waitForElementToBeVisible(cartCounterNumber);
            String countText = getTextFromElement(cartCounterNumber).trim();
            if (countText.isEmpty()) {
                logger.warn("Cart count element is visible but empty. Re-checking...");
                try {
                    wait.withTimeout(Duration.ofSeconds(3)).until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElement(cartCounterNumber, "")));
                    countText = getTextFromElement(cartCounterNumber).trim();
                    logger.info("Re-checked cart count text: '{}'", countText);
                    if (countText.isEmpty()) return 0; // Still empty after wait
                } catch (TimeoutException te) {
                    logger.error("Cart count text remained empty after extra wait.");
                    return 0;
                }
            }
            return Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            logger.error("Could not parse cart count text: '{}'", getTextFromElement(cartCounterNumber), e);
            return -1;
        } catch (NoSuchElementException e) {
            logger.info("Cart counter number element not found, assuming cart is empty.");
            return 0;
        } catch (Exception e) {
            logger.error("Unexpected error getting cart count.", e);
            return -1;
        }
    }

    /**
     * Explicitly waits for the cart counter number to display the expected count.
     * THIS METHOD IS PUBLIC as it needs to be called from the test class.
     *
     * @param expectedCount    The number of items expected in the cart.
     * @param timeoutInSeconds The maximum time to wait.
     */
    public void waitForCartCountToBe(int expectedCount, int timeoutInSeconds) {
        logger.info("Waiting for cart count to become {} (max {} seconds)...", expectedCount, timeoutInSeconds);
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
        try {
            if (expectedCount == 0) {
                customWait.until(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(cartCounterEmptyClassLocator), ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".counter.qty .counter-number"))));
                logger.info("Cart count is 0 (empty class or number invisible).");
            } else {
                customWait.until(ExpectedConditions.and(ExpectedConditions.visibilityOf(cartCounterNumber), // Ensure number element is visible
                        ExpectedConditions.textToBePresentInElement(cartCounterNumber, String.valueOf(expectedCount)) // Ensure text matches
                ));
                logger.info("Cart count successfully updated to {}.", expectedCount);
            }
        } catch (TimeoutException e) {
            int currentCount = -1;
            try {
                currentCount = getCartCount();
            } catch (Exception ignored) {
            }
            logger.error("Timeout waiting for cart count to become {}. Current count: {}", expectedCount, currentCount, e);
            // Assertion error is better handled in the test itself after calling this wait
            // fail("Timeout waiting for cart count to update to " + expectedCount);
            throw e; // Re-throw timeout so the test knows waiting failed
        } catch (Exception e) {
            logger.error("Unexpected error while waiting for cart count to be {}.", expectedCount, e);
            throw e; // Re-throw other errors
        }
    }

    public String getCartSubtotal() {
        expandCartIfNeeded();
        try {
            waitForElementToBeVisible(cartSubtotalPrice);
            return getTextFromElement(cartSubtotalPrice);
        } catch (NoSuchElementException e) {
            logger.warn("Cart subtotal element not found (cart might be empty).");
            return "$0.00";
        }
    }

    public List<String> getCartItemProductTitles() {
        expandCartIfNeeded();
        try {
            if (getCartCount() > 0) {
                wait.until(ExpectedConditions.visibilityOfAllElements(cartItemNames));
                return cartItemNames.stream().map(WebElement::getText).collect(Collectors.toList());
            } else {
                logger.info("Cart is empty, returning empty list of item names.");
                return List.of();
            }
        } catch (NoSuchElementException | TimeoutException e) {
            logger.warn("No item names found in mini-cart or timed out waiting.", e);
            return List.of();
        }
    }

    private void expandCartIfNeeded() {
        boolean isActive = minicartWrapper.getAttribute("class").contains("active");
        logger.debug("Checking if cart needs expansion. Active class present: {}", isActive);
        if (!isActive) {
            logger.debug("Cart not expanded based on class, clicking icon.");
            expandCart();
        } else {
            try {
                // Check if content (items or empty message) is visible
                wait.withTimeout(Duration.ofSeconds(5)).until(ExpectedConditions.or(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#mini-cart .product-item-details")), ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".minicart-items-wrapper .subtitle.empty"))));
                logger.debug("Cart is active and content (items or empty msg) is visible.");
            } catch (Exception e) {
                logger.warn("Cart seemed active, but content didn't load quickly. Attempting expansion again.", e);
                expandCart();
            }
        }
    }
}