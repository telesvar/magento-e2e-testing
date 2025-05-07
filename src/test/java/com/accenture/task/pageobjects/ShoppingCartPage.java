package com.accenture.task.pageobjects;

import com.accenture.task.utils.TestUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCartPage extends BasePage {

    // Locators relative to a cart item row (tbody)
    private final By productNameLocator = By.cssSelector("td.col.item .product-item-name a");
    private final By itemPriceLocator = By.cssSelector("td.col.price .cart-price .price");
    private final By itemQtyInputLocator = By.cssSelector("td.col.qty input.qty");
    private final By itemSubtotalLocator = By.cssSelector("td.col.subtotal .cart-price .price");
    @FindBy(css = ".cart.item") // Selects each row (tbody) in the cart table
    private List<WebElement> cartItemRows;
    @FindBy(css = ".grand.totals .price") // Price in the grand total row
    private WebElement grandTotalPrice;
    @FindBy(css = ".page-title span.base") // Page title H1
    private WebElement pageTitle;

    public ShoppingCartPage(WebDriver driver) {
        super(driver);
        waitForElementToBeVisible(pageTitle); // Ensure page title is loaded
    }

    public String getPageTitle() {
        return getTextFromElement(pageTitle);
    }

    /**
     * Gets the grand total price from the cart summary.
     *
     * @return The grand total as a double.
     */
    public double getGrandTotal() {
        waitForElementToBeVisible(grandTotalPrice);
        String totalText = getTextFromElement(grandTotalPrice);
        return TestUtils.extractPrice(totalText);
    }

    /**
     * Retrieves details for all items currently displayed in the shopping cart table.
     *
     * @return A Map where the key is the product name and the value is CartItemDetails.
     */
    public Map<String, CartItemDetails> getCartItemDetails() {
        Map<String, CartItemDetails> items = new HashMap<>();
        // Wait for the table itself or the first row to be present
        try {
            wait.until(ExpectedConditions.or(ExpectedConditions.visibilityOfElementLocated(By.id("shopping-cart-table")), ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".cart.empty")) // Handle empty cart case
            ));
        } catch (Exception e) {
            logger.error("Shopping cart table or empty message did not appear.", e);
            return items; // Return empty map if table isn't there
        }


        // Refresh the list of rows after waiting
        cartItemRows = driver.findElements(By.cssSelector(".cart.item"));

        if (cartItemRows.isEmpty()) {
            logger.info("Shopping cart table is present but contains no item rows.");
            return items; // Return empty if no rows found
        }

        logger.info("Found {} item rows in the cart.", cartItemRows.size());

        for (WebElement row : cartItemRows) {
            try {
                // It's safer to wait briefly for elements within the row, though they should be loaded
                WebElement nameElement = wait.until(ExpectedConditions.visibilityOf(row.findElement(productNameLocator)));
                WebElement priceElement = row.findElement(itemPriceLocator);
                WebElement qtyElement = row.findElement(itemQtyInputLocator);
                WebElement subtotalElement = row.findElement(itemSubtotalLocator);

                String name = nameElement.getText();
                String priceText = priceElement.getText();
                String qtyText = qtyElement.getAttribute("value");
                String subtotalText = subtotalElement.getText();

                logger.debug("Processing cart item: Name='{}', Price='{}', Qty='{}', Subtotal='{}'", name, priceText, qtyText, subtotalText);

                items.put(name, new CartItemDetails(name, TestUtils.extractPrice(priceText), Integer.parseInt(qtyText), TestUtils.extractPrice(subtotalText)));
            } catch (NoSuchElementException | NumberFormatException e) {
                logger.error("Error parsing details for a cart row. Skipping row.", e);
                // Optionally add a placeholder or skip the row
            } catch (Exception e) {
                logger.error("Unexpected error processing cart row.", e);
            }
        }
        logger.info("Extracted details for {} items from the cart page.", items.size());
        return items;
    }

    /**
     * Inner class to hold structured data for a cart item.
     */
    public static class CartItemDetails {
        public final String name;
        public final double price;
        public final int quantity;
        public final double subtotal;

        public CartItemDetails(String name, double price, int quantity, double subtotal) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        @Override
        public String toString() {
            return "CartItemDetails{" + "name='" + name + '\'' + ", price=" + price + ", quantity=" + quantity + ", subtotal=" + subtotal + '}';
        }
    }
}