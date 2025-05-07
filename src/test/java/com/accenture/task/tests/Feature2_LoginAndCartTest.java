package com.accenture.task.tests;

import com.accenture.task.pageobjects.*;
import com.accenture.task.utils.TestUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Feature2_LoginAndCartTest {

    private static final String BASE_URL = "https://magento.softwaretestingboard.com/";
    private static final Logger logger = LoggerFactory.getLogger(Feature2_LoginAndCartTest.class);

    private static final String CATEGORY_URL_1_ASC = BASE_URL + "gear/bags.html";
    private static final String CATEGORY_URL_2_DESC = BASE_URL + "men/tops-men.html";
    private static final String CATEGORY_URL_3_MIN_MAX = BASE_URL + "women/tops-women.html";
    private static final String PARTIAL_SEARCH_TERM = "bag";
    private static final String FULL_SEARCH_TERM_PRODUCT = "Wayfarer Messenger Bag";
    private static final String userPassword = "Password123!";
    private static final List<ProductInfoForCart> productsExpectedInCart = new ArrayList<>();
    private static final AtomicInteger expectedCartCount = new AtomicInteger(0);

    private static String userEmail;
    private static String userFirstName;
    private static String userLastName;
    private static boolean isUserLoggedIn = false;

    private WebDriver driver;
    private WebDriverWait wait;
    private HomePage homePage;

    @BeforeAll
    void setupTestSuite() {
        logger.info("Setting up WebDriverManager and registering user ONCE for Feature 2...");
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        WebDriver setupDriver = new ChromeDriver(options);

        try {
            // Register the user needed for the tests
            userEmail = TestUtils.generateUniqueEmail();
            userFirstName = "Feat2FN" + TestUtils.generateRandomString(3);
            userLastName = "Feat2LN" + TestUtils.generateRandomString(3);
            logger.info("Registering user: {} {} ({}) with password '{}'", userFirstName, userLastName, userEmail, userPassword);

            setupDriver.get(BASE_URL);
            HomePage setupHomePage = new HomePage(setupDriver);
            CreateAccountPage setupCreateAccountPage = setupHomePage.getHeader().clickCreateAccount();
            AccountPage setupAccountPage = setupCreateAccountPage.registerUser(userFirstName, userLastName, userEmail, userPassword);

            // Robust verification of registration success
            assertNotNull(setupAccountPage, "Setup failed: registerUser returned null AccountPage.");
            assertEquals("My Account", setupAccountPage.getPageTitle(), "Setup failed: Did not land on Account Page.");
            assertThat("Setup failed: Registration success message not found.", setupAccountPage.getSuccessMessage(), containsString("Thank you for registering"));
            logger.info("User registration successful for Feature 2.");

        } catch (Exception e) {
            logger.error("!!! FAILED TO REGISTER USER FOR FEATURE 2 TESTS !!!", e);
            fail("User registration failed in @BeforeAll", e);
        } finally {
            setupDriver.quit();
        }

        // Now initialize the main driver for the actual tests
        logger.info("Initializing main WebDriver for Feature 2 tests...");
        driver = new ChromeDriver(options); // Use the same options
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.manage().window().maximize();
        homePage = new HomePage(driver); // Initialize HomePage for the tests
        homePage.navigateToHomePage(BASE_URL);

        productsExpectedInCart.clear();
        expectedCartCount.set(0);
        isUserLoggedIn = false; // Ensure starts as logged out for the first test
        logger.info("Main WebDriver initialized for tests.");
    }

    @AfterAll
    void tearDownTestSuite() {
        if (driver != null) {
            logger.info("Quitting main WebDriver after Feature 2 tests...");
            driver.quit();
            logger.info("Main WebDriver quit successfully.");
        }
        productsExpectedInCart.clear();
        expectedCartCount.set(0);
        isUserLoggedIn = false;
    }

    @Test
    @Order(1)
    @DisplayName("Login: Successfully log in")
    void testSuccessfulLogin() {
        logger.info("Starting successful login test for user: {}", userEmail);
        SignInPage signInPage = homePage.getHeader().clickSignIn();
        signInPage.attemptLogin(userEmail, userPassword);

        try {
            // Wait for redirection AND the welcome message in the header
            wait.until(ExpectedConditions.or(ExpectedConditions.urlToBe(BASE_URL), // Redirects to home
                    ExpectedConditions.urlContains("customer/account") // Or account page
            ));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".greet.welcome .logged-in")));
        } catch (TimeoutException e) {
            String loginError = signInPage.getLoginErrorMessage();
            fail("Login failed: Did not redirect correctly or welcome message did not appear. Login Page Error: '" + loginError + "'. Current URL: " + driver.getCurrentUrl(), e);
        }

        // Re-initialize HomePage to ensure header state is current
        homePage = new HomePage(driver);
        assertTrue(homePage.getHeader().isUserLoggedIn(), "Header should indicate user is logged in after successful login.");
        String welcomeMsg = homePage.getHeader().getWelcomeMessage();
        assertThat("Welcome message should contain user's first name", welcomeMsg, containsString(userFirstName));
        assertThat("Welcome message should contain user's last name", welcomeMsg, containsString(userLastName));

        isUserLoggedIn = true; // Set flag for subsequent tests
        logger.info("Successful login test completed.");
    }

    @Test
    @Order(2)
    @DisplayName("FEATURE 2 / Task 1: Sort by Price Ascending and Verify First Product Price")
    void testSortPriceAscending() {
        assumeUserIsLoggedIn();
        logger.info("Starting sort price ascending test in category: {}", CATEGORY_URL_1_ASC);
        driver.get(CATEGORY_URL_1_ASC);
        ProductListPage productListPage = new ProductListPage(driver);
        productListPage.selectSortBy("Price");
        productListPage.setSortDirection("asc");
        double firstPrice = productListPage.getFirstProductPrice();
        logger.info("Price of first product after sorting ascending: {}", firstPrice);
        assertThat("First product price should be non-negative", firstPrice, greaterThanOrEqualTo(0.0));
        logger.info("Sort price ascending test completed.");
    }

    @Test
    @Order(3)
    @DisplayName("FEATURE 2 / Task 2: Sort by Price Descending and Verify First Product Price")
    void testSortPriceDescending() {
        assumeUserIsLoggedIn();
        logger.info("Starting sort price descending test in category: {}", CATEGORY_URL_2_DESC);
        driver.get(CATEGORY_URL_2_DESC);
        ProductListPage productListPage = new ProductListPage(driver);
        productListPage.selectSortBy("Price");
        productListPage.setSortDirection("desc");
        double firstPrice = productListPage.getFirstProductPrice();
        logger.info("Price of first product after sorting descending: {}", firstPrice);
        assertThat("First product price should be non-negative", firstPrice, greaterThanOrEqualTo(0.0));
        logger.info("Sort price descending test completed.");
    }

    @Test
    @Order(4)
    @DisplayName("FEATURE 2 / Task 3: Search by Partial Title and Verify Result")
    void testSearchByPartialTitle() {
        assumeUserIsLoggedIn();
        logger.info("Starting search by partial title test: '{}'", PARTIAL_SEARCH_TERM);
        ProductListPage resultsPage = homePage.getHeader().searchFor(PARTIAL_SEARCH_TERM);
        List<WebElement> products = resultsPage.getProductItems();
        assertThat("Search should return at least one product", products, is(not(empty())));
        // Find container within the first item
        WebElement firstProductContainer = resultsPage.getProductItems().getFirst().findElement(By.cssSelector(".product-item-info"));
        String firstProductName = resultsPage.getProductName(firstProductContainer);
        logger.info("First product found by partial search: {}", firstProductName);
        assertThat("First product name should contain search term (case-insensitive)", firstProductName.toLowerCase(), containsString(PARTIAL_SEARCH_TERM.toLowerCase()));
        logger.info("Search by partial title test completed.");
    }

    @Test
    @Order(5)
    @DisplayName("FEATURE 2 / Task 3: Search by Full Title and Verify Result & Store")
    void testSearchByFullTitleAndStore() {
        assumeUserIsLoggedIn();
        logger.info("Starting search by full title test: '{}'", FULL_SEARCH_TERM_PRODUCT);
        ProductListPage resultsPage = homePage.getHeader().searchFor(FULL_SEARCH_TERM_PRODUCT);
        List<WebElement> products = resultsPage.getProductItems();
        assertThat("Search for full title should return at least one product", products, is(not(empty())));
        WebElement productContainer = resultsPage.findProductContainerByName(FULL_SEARCH_TERM_PRODUCT);
        String productName = resultsPage.getProductName(productContainer);
        logger.info("Product found by full search: {}", productName);
        assertThat("Product name should match full search term", productName, equalToIgnoringCase(FULL_SEARCH_TERM_PRODUCT));

        double price = resultsPage.getProductPrice(productContainer);
        // Add to static list ONLY IF NOT ALREADY ADDED
        if (productsExpectedInCart.stream().noneMatch(p -> p.name.equalsIgnoreCase(productName))) {
            productsExpectedInCart.add(new ProductInfoForCart(productName, price, 1));
            logger.info("Stored searched product '{}' (Price: {}) for later cart verification.", productName, price);
        } else {
            logger.warn("Product '{}' already in expected list from previous step, not adding again.", productName);
        }
        logger.info("Search by full title test completed.");
    }

    @Test
    @Order(6)
    @DisplayName("FEATURE 2 / Task 4: Add Min/Max Price Products from Third Category")
    void testAddMinMaxPriceProducts() {
        assumeUserIsLoggedIn();
        int countBefore = expectedCartCount.get(); // Use atomic integer
        logger.info("Starting add min/max price products test from category: {}. Current expected cart count: {}", CATEGORY_URL_3_MIN_MAX, countBefore);

        // Add Max Price Product
        addMaxPriceProductFromCategory(countBefore);

        // Add Min Price Product
        // Get the *updated* count before adding the next item
        int countBeforeMin = expectedCartCount.get();
        addMinPriceProductFromCategory(countBeforeMin);

        logger.info("Add min/max price products test completed. Final expected count: {}", expectedCartCount.get());
    }

    @Test
    @Order(7)
    @DisplayName("FEATURE 2 / Task 4: Add Searched Product to Cart")
    void testAddSearchedProductToCart() {
        assumeUserIsLoggedIn();
        int countBeforeAdd = expectedCartCount.get();
        logger.info("Starting add searched product test. Current expected cart count: {}", countBeforeAdd);

        // Find the previously stored searched product info
        ProductInfoForCart searchedProductInfo = productsExpectedInCart.stream().filter(p -> p.name.equalsIgnoreCase(FULL_SEARCH_TERM_PRODUCT)).findFirst().orElseThrow(() -> new AssertionError("Searched product '" + FULL_SEARCH_TERM_PRODUCT + "' was not found in expected list. Was testSearchByFullTitleAndStore run successfully?"));
        logger.info("Attempting to add previously searched product: '{}'", searchedProductInfo.name);

        ProductListPage resultsPage = homePage.getHeader().searchFor(searchedProductInfo.name);
        ProductDetailPage detailPage = resultsPage.selectProductByName(searchedProductInfo.name);

        addProductToCartFromDetailPage(detailPage, countBeforeAdd);
        // Note: We don't add to productsExpectedInCart here again, it was added in Order 5

        logger.info("Add searched product test completed. Final expected count: {}", expectedCartCount.get());
    }

    @Test
    @Order(8)
    @DisplayName("FEATURE 2 / Task 4: Verify Final Cart Contents (Titles, Qty, Price, Total)")
    void testVerifyFinalCartContents() {
        assumeUserIsLoggedIn();
        int finalExpectedCount = expectedCartCount.get(); // Get final expected count
        assertThat("Expected product info list should not be empty for final verification", productsExpectedInCart, is(not(empty())));
        logger.info("Starting final cart content verification. Expected items in list: {}. Expected final count: {}", productsExpectedInCart.size(), finalExpectedCount);
        logger.debug("Expected cart details list: {}", productsExpectedInCart);

        // Navigate explicitly to ensure we are on the cart page
        driver.get(BASE_URL + "checkout/cart/");
        ShoppingCartPage cartPage = new ShoppingCartPage(driver);
        assertThat("Should be on Shopping Cart page", cartPage.getPageTitle(), is(equalToIgnoringCase("Shopping Cart")));

        Map<String, ShoppingCartPage.CartItemDetails> actualCartItems = cartPage.getCartItemDetails();
        logger.info("Actual items found in cart: {}", actualCartItems.size());
        logger.debug("Actual cart details map: {}", actualCartItems);

        // Allow for slight discrepancy if add-to-cart failed silently but wasn't caught
        // Assert based on the tracked expected count
        assertThat("Actual cart item count should match final tracked expected count", actualCartItems.size(), is(equalTo(finalExpectedCount)));
        // Also check against the size of the list we built
        assertThat("Actual cart item count should match expected list size", actualCartItems.size(), is(equalTo(productsExpectedInCart.size())));
        
        double calculatedExpectedTotal = 0.0;
        for (ProductInfoForCart expectedItem : productsExpectedInCart) {
            ShoppingCartPage.CartItemDetails actualItem = actualCartItems.get(expectedItem.name);
            assertNotNull(actualItem, "Product '" + expectedItem.name + "' was expected but not found in the cart.");
            assertThat("Quantity for product '" + expectedItem.name + "'", actualItem.quantity, is(equalTo(expectedItem.quantity)));
            assertThat("Price for product '" + expectedItem.name + "'", actualItem.price, is(closeTo(expectedItem.price, 0.01)));
            double expectedSubtotal = actualItem.price * actualItem.quantity; // Use actual price from cart for calculation
            assertThat("Subtotal for product '" + expectedItem.name + "'", actualItem.subtotal, is(closeTo(expectedSubtotal, 0.01)));
            calculatedExpectedTotal += actualItem.subtotal;
        }

        // Only assert total if items were actually found
        if (!actualCartItems.isEmpty()) {
            double actualGrandTotal = cartPage.getGrandTotal();
            logger.info("Calculated Expected Grand Total (sum of subtotals): {}, Actual Grand Total from page: {}", calculatedExpectedTotal, actualGrandTotal);
            assertThat("Cart grand total should match calculated total", actualGrandTotal, is(closeTo(calculatedExpectedTotal, 0.02)));
        } else {
            logger.warn("Skipping grand total check as no items were found in the cart.");
        }

        logger.info("Final cart content verification completed successfully.");
    }

    private void assumeUserIsLoggedIn() {
        Assumptions.assumeTrue(isUserLoggedIn, "User login failed or did not complete in the first test. Skipping test.");
    }

    private void addMaxPriceProductFromCategory(int countBeforeAdd) {
        logger.info("Attempting to add MAX price product from {}...", Feature2_LoginAndCartTest.CATEGORY_URL_3_MIN_MAX);
        driver.get(Feature2_LoginAndCartTest.CATEGORY_URL_3_MIN_MAX);
        ProductListPage productListPage = new ProductListPage(driver);
        productListPage.selectSortBy("Price");
        productListPage.setSortDirection("desc");
        ProductDetailPage detailPage = productListPage.selectProductWithMaxPrice();
        addProductToCartFromDetailPage(detailPage, countBeforeAdd);
    }

    private void addMinPriceProductFromCategory(int countBeforeAdd) {
        logger.info("Attempting to add MIN price product from {}...", Feature2_LoginAndCartTest.CATEGORY_URL_3_MIN_MAX);
        driver.get(Feature2_LoginAndCartTest.CATEGORY_URL_3_MIN_MAX);
        ProductListPage productListPage = new ProductListPage(driver);
        productListPage.selectSortBy("Price");
        productListPage.setSortDirection("asc");
        ProductDetailPage detailPage = productListPage.selectProductWithMinPrice();
        addProductToCartFromDetailPage(detailPage, countBeforeAdd);
    }

    private void addProductToCartFromDetailPage(ProductDetailPage detailPage, int countBeforeAdd) {
        String productName = detailPage.getProductName();
        double productPrice = detailPage.getProductPrice();
        int expectedCountAfterAdd = countBeforeAdd + 1;

        logger.info("Adding product: {} (${})", productName, productPrice);
        if (detailPage.hasSizeOptions()) detailPage.selectFirstAvailableSize();
        if (detailPage.hasColorOptions()) detailPage.selectFirstAvailableColor();
        detailPage.clickAddToCart();

        // Check for "qty not available" error FIRST
        String errorMessage = detailPage.getErrorMessage();
        if (errorMessage.contains("The requested qty is not available")) {
            logger.error("Product '{}' could not be added - Qty not available.", productName);
            // Fail the test immediately if the item cannot be added as expected
            fail("Failed to add product '" + productName + "' because requested quantity is not available.");
        }
        // Check for OTHER unexpected errors
        else if (!errorMessage.isEmpty()) {
            logger.error("An unexpected error occurred while adding '{}' to cart: {}", productName, errorMessage);
            fail("Unexpected error adding product '" + productName + "': " + errorMessage);
        }

        // Wait for cart count to update ONLY if no error was detected
        try {
            homePage.getHeader().waitForCartCountToBe(expectedCountAfterAdd, 15);
            // If wait succeeds, update state
            if (productsExpectedInCart.stream().noneMatch(p -> p.name.equalsIgnoreCase(productName))) {
                productsExpectedInCart.add(new ProductInfoForCart(productName, productPrice, 1));
                logger.info("Stored product '{}' in expected list.", productName);
            } else {
                // If product was already expected (like the searched item), update its quantity if needed (though not required by spec)
                logger.warn("Product '{}' already in expected list. Assuming quantity remains 1 for this test.", productName);
            }
            expectedCartCount.incrementAndGet(); // Increment expected count AFTER successful add confirmed by wait
            logger.info("Cart count updated to {}. Product '{}' added successfully.", expectedCountAfterAdd, productName);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for cart count to update to {} after adding '{}'.", expectedCountAfterAdd, productName);
            fail("Cart count did not update correctly after adding product: " + productName, e);
        }
    }

    private static class ProductInfoForCart {
        String name;
        double price;
        int quantity;

        ProductInfoForCart(String name, double price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "ProductInfoForCart{name='" + name + "', price=" + price + ", quantity=" + quantity + '}';
        }
    }
}