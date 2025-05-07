package com.accenture.task.pageobjects;

import com.accenture.task.utils.TestUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProductListPage extends BasePage {

    private final By productItemPriceLocator = By.cssSelector(".price-box .price, .price-box .minimal-price .price");
    private final By productItemNameLocator = By.cssSelector(".product-item-link");
    private final By productItemContainerLocator = By.cssSelector(".product-item-info");
    @FindBy(id = "sorter")
    private WebElement sorterDropdown;
    @FindBy(css = ".toolbar-sorter .sorter-action")
    private WebElement sortDirectionLink;
    @FindBy(css = ".product-items .product-item")
    private List<WebElement> productItems;
    @FindBy(css = ".page-title span.base")
    private WebElement pageOrCategoryTitle;

    public ProductListPage(WebDriver driver) {
        super(driver);
        waitForPageToLoad();
    }

    private void waitForPageToLoad() {
        try {
            wait.until(ExpectedConditions.or(ExpectedConditions.visibilityOf(pageOrCategoryTitle), ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".products.wrapper.grid"))));
            logger.debug("Product List Page loaded (title or grid wrapper visible).");
            productItems = driver.findElements(By.cssSelector(".product-items .product-item"));
        } catch (Exception e) {
            logger.error("Product List Page did not load correctly.", e);
            throw e;
        }
    }

    public String getPageTitle() {
        waitForElementToBeVisible(pageOrCategoryTitle);
        return getTextFromElement(pageOrCategoryTitle);
    }

    public void selectSortBy(String optionText) {
        waitForElementToBeVisible(sorterDropdown);
        Select select = new Select(sorterDropdown);
        logger.info("Selecting sort option: {}", optionText);
        select.selectByVisibleText(optionText);
        try {
            logger.debug("Waiting briefly after selecting sort by...");
            Thread.sleep(1000); // Small pause often helps
            waitForProductsToLoadAfterAction();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setSortDirection(String direction) {
        waitForElementToBeVisible(sortDirectionLink);
        String currentCssClass = sortDirectionLink.getAttribute("class");
        boolean isCurrentlyAsc = currentCssClass.contains("sort-asc");
        boolean needsClick = (direction.equalsIgnoreCase("desc") && isCurrentlyAsc) || (direction.equalsIgnoreCase("asc") && !isCurrentlyAsc);

        logger.info("Current sort direction link class: '{}'. Is ASC: {}. Needs click for '{}': {}", currentCssClass, isCurrentlyAsc, direction, needsClick);

        if (needsClick) {
            logger.info("Clicking sort direction link to set to '{}'", direction);
            // Use robust clickElement which includes JS fallback
            clickElement(sortDirectionLink); // This now handles interception better
            waitForProductsToLoadAfterAction();
        } else {
            logger.info("Sort direction is already set correctly for '{}'", direction);
        }
    }

    private void waitForProductsToLoadAfterAction() {
        logger.debug("Waiting for products to reload after action...");
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".products.wrapper.grid")));
            if (!productItems.isEmpty()) {
                try {
                    logger.trace("Waiting for staleness of old first product item...");
                    wait.withTimeout(Duration.ofSeconds(5)).until(ExpectedConditions.stalenessOf(productItems.get(0)));
                    logger.trace("Old product item became stale.");
                } catch (TimeoutException e) {
                    logger.warn("Old product item did not become stale quickly, proceeding to check for new items visibility.");
                }
            }
            // Wait for the container to have at least one item, or just be present
            wait.until(ExpectedConditions.or(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(".product-items .product-item"), 0), ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-items"))));
            productItems = driver.findElements(By.cssSelector(".product-items .product-item"));
            logger.debug("Products reloaded. Found {} items.", productItems.size());
        } catch (Exception e) {
            logger.warn("Exception while waiting for products to reload after action. Attempting to re-find.", e);
            productItems = driver.findElements(By.cssSelector(".product-items .product-item"));
        }
    }


    public List<WebElement> getProductItems() {
        try {
            wait.until(ExpectedConditions.or(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(".product-items .product-item"), 0), ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-items"))));
            productItems = driver.findElements(By.cssSelector(".product-items .product-item"));
        } catch (Exception e) {
            logger.warn("Could not find product items or list is empty after wait.", e);
            productItems = new ArrayList<>();
        }
        return productItems;
    }

    public String getProductName(WebElement productItemContainer) {
        try {
            return productItemContainer.findElement(productItemNameLocator).getText();
        } catch (NoSuchElementException e) {
            logger.error("Could not find product name link within item container.", e);
            return "N/A";
        }
    }

    public double getProductPrice(WebElement productItemContainer) {
        try {
            WebElement priceElement = productItemContainer.findElement(productItemPriceLocator);
            String priceText = priceElement.getText();
            return TestUtils.extractPrice(priceText);
        } catch (NoSuchElementException e) {
            logger.warn("Could not find standard price element ({}). Trying 'Starting at' price.", productItemPriceLocator, e);
            try {
                WebElement minimalPriceElement = productItemContainer.findElement(By.cssSelector(".minimal-price .price"));
                String minimalPriceText = minimalPriceElement.getText();
                logger.info("Found 'Starting at' price: {}", minimalPriceText);
                return TestUtils.extractPrice(minimalPriceText);
            } catch (NoSuchElementException e2) {
                logger.error("Could not find standard OR 'Starting at' price within item container.", e2);
                return -1.0;
            }
        }
    }

    public ProductDetailPage clickProduct(WebElement productItemContainer) {
        WebElement nameLink = productItemContainer.findElement(productItemNameLocator);
        String productName = nameLink.getText();
        logger.info("Clicking product: {}", productName);
        clickElement(nameLink);
        return new ProductDetailPage(driver);
    }

    private List<ProductInfo> getAllProductInfo() {
        waitForProductsToLoadAfterAction();
        List<ProductInfo> productInfos = new ArrayList<>();
        List<WebElement> currentItems = getProductItems();

        if (currentItems.isEmpty()) {
            logger.warn("No product items found on page to extract info.");
            return productInfos;
        }

        for (WebElement item : currentItems) {
            WebElement itemInfoContainer;
            try {
                itemInfoContainer = item.findElement(productItemContainerLocator);
            } catch (NoSuchElementException e) {
                logger.warn("Could not find '.product-item-info' container within a list item. Skipping item.");
                continue;
            }
            double price = getProductPrice(itemInfoContainer);
            String name = getProductName(itemInfoContainer);
            if (price >= 0 && !name.equals("N/A")) {
                productInfos.add(new ProductInfo(itemInfoContainer, price, name));
            } else {
                logger.warn("Skipping product '{}' due to invalid price ({}) or name.", name, price);
            }
        }
        logger.debug("Extracted valid info for {} products.", productInfos.size());
        return productInfos;
    }

    public ProductDetailPage selectProductWithMinPrice() {
        List<ProductInfo> products = getAllProductInfo();
        if (products.isEmpty()) {
            throw new NoSuchElementException("No valid products found on the page to determine min price.");
        }
        products.sort(Comparator.comparingDouble((ProductInfo p) -> p.price));
        ProductInfo minPriceProduct = products.get(0);
        logger.info("Selecting product with min price: {} (${})", minPriceProduct.name, minPriceProduct.price);
        return clickProduct(minPriceProduct.element);
    }

    public ProductDetailPage selectProductWithMaxPrice() {
        List<ProductInfo> products = getAllProductInfo();
        if (products.isEmpty()) {
            throw new NoSuchElementException("No valid products found on the page to determine max price.");
        }
        products.sort(Comparator.comparingDouble((ProductInfo p) -> p.price).reversed());
        ProductInfo maxPriceProduct = products.get(0);
        logger.info("Selecting product with max price: {} (${})", maxPriceProduct.name, maxPriceProduct.price);
        return clickProduct(maxPriceProduct.element);
    }

    public double getFirstProductPrice() {
        waitForProductsToLoadAfterAction();
        List<WebElement> currentItems = getProductItems();
        if (currentItems.isEmpty()) {
            throw new NoSuchElementException("No product items found on the page.");
        }
        WebElement firstItemInfoContainer = currentItems.get(0).findElement(productItemContainerLocator);
        return getProductPrice(firstItemInfoContainer);
    }

    public WebElement findProductContainerByName(String name) {
        waitForProductsToLoadAfterAction();
        List<WebElement> currentItems = getProductItems();
        logger.debug("Searching for product '{}' among {} items", name, currentItems.size());
        for (WebElement item : currentItems) {
            WebElement itemInfoContainer;
            try {
                itemInfoContainer = item.findElement(productItemContainerLocator);
                String currentName = getProductName(itemInfoContainer);
                logger.trace("Checking item: {}", currentName);
                if (currentName.equalsIgnoreCase(name)) {
                    logger.info("Found product container for: {}", name);
                    return itemInfoContainer;
                }
            } catch (NoSuchElementException e) {
                logger.warn("Skipping item, could not find inner container or name.");
            }
        }
        throw new NoSuchElementException("Product with name '" + name + "' not found on the current list page.");
    }

    public ProductDetailPage selectProductByName(String name) {
        WebElement productItemContainer = findProductContainerByName(name);
        return clickProduct(productItemContainer);
    }

    public ProductListPage searchForProduct(String searchTerm) {
        return getHeader().searchFor(searchTerm);
    }

    private static class ProductInfo {
        WebElement element;
        double price;
        String name;

        ProductInfo(WebElement element, double price, String name) {
            this.element = element;
            this.price = price;
            this.name = name;
        }
    }
}