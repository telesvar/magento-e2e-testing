package com.accenture.task.utils;

import java.util.Random;

public class TestUtils {

    /**
     * Generates a pseudo-unique email address using the current timestamp.
     * Suitable for test environments where true uniqueness isn't strictly required across runs.
     *
     * @return A unique email string like "testuser_1678886400000@example.com"
     */
    public static String generateUniqueEmail() {
        long timestamp = System.currentTimeMillis();
        return "testuser_" + timestamp + "@example.com";
    }

    /**
     * Generates a random string of specified length using alphabetic characters.
     *
     * @param length The desired length of the random string.
     * @return A random alphabetic string.
     */
    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    /**
     * Extracts a double value from a price string (e.g., "$45.00").
     * Removes currency symbols and commas.
     *
     * @param priceText The price string.
     * @return The extracted price as a double, or 0.0 if parsing fails.
     */
    public static double extractPrice(String priceText) {
        if (priceText == null || priceText.isEmpty()) {
            System.err.println("Cannot parse null or empty price string.");
            return 0.0;
        }
        // Remove currency symbols ($), commas (,) etc. Keep the decimal point.
        String cleanedPrice = priceText.replaceAll("[^\\d.]", "");
        try {
            if (cleanedPrice.isEmpty()) {
                System.err.println("Price string became empty after cleaning: " + priceText);
                return 0.0;
            }
            return Double.parseDouble(cleanedPrice);
        } catch (NumberFormatException e) {
            System.err.println("Could not parse price string after cleaning: '" + cleanedPrice + "' (Original: '" + priceText + "')");
            return 0.0; // Or throw an exception depending on desired behavior
        }
    }
}