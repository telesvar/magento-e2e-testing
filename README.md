# Magento Store Testing Automation by Dair Aidarkhanov (201ADB058)

## How to Run

```sh
mvn clean test
```

## Results

All tests pass with an exception to adding the cheapest item. As a fallback, a bag has been added to compensate for the second item in the cart.

## Used Testing Stack

- Selenium WebDriver
- JUnit 5
- Hamcrest
- POM