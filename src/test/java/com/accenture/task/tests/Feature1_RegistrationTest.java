package com.accenture.task.tests;

import com.accenture.task.pageobjects.AccountPage;
import com.accenture.task.pageobjects.CreateAccountPage;
import com.accenture.task.utils.TestUtils;
import org.junit.jupiter.api.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Feature1_RegistrationTest extends BaseTest {

    private static final String EXISTING_EMAIL = "test_already_exists@example.com";
    private static final String EXISTING_FIRST_NAME = "Existing";
    private static final String EXISTING_LAST_NAME = "User";
    private static final String VALID_PASSWORD = "Password123!";

    @Test
    @Order(1)
    @DisplayName("FEATURE 1 / Task 1: Verify successful registration with unique email")
    void testSuccessfulRegistration() {
        String uniqueEmail = TestUtils.generateUniqueEmail();
        String firstName = "TestFN" + TestUtils.generateRandomString(3);
        String lastName = "TestLN" + TestUtils.generateRandomString(3);
        logger.info("Starting successful registration test with email: {} for user: {} {}", uniqueEmail, firstName, lastName);

        // Use getHeader() from BasePage (via homePage)
        CreateAccountPage createAccountPage = homePage.getHeader().clickCreateAccount();
        assertThat("Should be on Create Account page", createAccountPage.getPageTitle(), is(equalToIgnoringCase("Create New Customer Account")));

        AccountPage accountPage = createAccountPage.registerUser(firstName, lastName, uniqueEmail, VALID_PASSWORD);

        assertThat("Should be redirected to Account Page", accountPage.getPageTitle(), is(equalToIgnoringCase("My Account")));
        assertThat("Success message should be displayed", accountPage.getSuccessMessage(), containsString("Thank you for registering"));

        String contactInfo = accountPage.getContactInfoText();
        logger.debug("Contact Info found: {}", contactInfo);
        assertThat("Contact info should contain first name", contactInfo, containsString(firstName));
        assertThat("Contact info should contain last name", contactInfo, containsString(lastName));
        assertThat("Contact info should contain email", contactInfo, containsString(uniqueEmail));
        logger.info("Successful registration test completed for {}", uniqueEmail);
    }

    @Test
    @Order(2)
    @DisplayName("FEATURE 1 / Task 2: Verify error message for existing email")
    void testRegistrationWithExistingEmail() {
        logger.info("Starting registration failure test with existing email: {}", EXISTING_EMAIL);

        // Use getHeader()
        CreateAccountPage createAccountPage = homePage.getHeader().clickCreateAccount();
        assertThat("Should be on Create Account page", createAccountPage.getPageTitle(), is(equalToIgnoringCase("Create New Customer Account")));

        createAccountPage.enterFirstName(EXISTING_FIRST_NAME);
        createAccountPage.enterLastName(EXISTING_LAST_NAME);
        createAccountPage.enterEmail(EXISTING_EMAIL);
        createAccountPage.enterPassword(VALID_PASSWORD);
        createAccountPage.enterConfirmPassword(VALID_PASSWORD);
        createAccountPage.clickCreateAccountButton();

        String generalError = createAccountPage.getGeneralErrorText();
        assertThat("General error message should indicate existing account", generalError, containsString("There is already an account with this email address"));
        assertThat("Should remain on Create Account page after failed registration", createAccountPage.getPageTitle(), is(equalToIgnoringCase("Create New Customer Account")));
        logger.info("Existing email registration failure test completed.");
    }

    @Test
    @Order(3)
    @DisplayName("FEATURE 1 / Task 2: Verify error messages for missing mandatory fields")
    void testRegistrationWithMissingFields() {
        logger.info("Starting registration failure test with missing fields");

        // Use getHeader()
        CreateAccountPage createAccountPage = homePage.getHeader().clickCreateAccount();
        assertThat("Should be on Create Account page", createAccountPage.getPageTitle(), is(equalToIgnoringCase("Create New Customer Account")));

        createAccountPage.clickCreateAccountButton();

        assertThat("First name error expected", createAccountPage.getFieldErrorText("firstname"), is("This is a required field."));
        assertThat("Last name error expected", createAccountPage.getFieldErrorText("lastname"), is("This is a required field."));
        assertThat("Email error expected", createAccountPage.getFieldErrorText("email_address"), is("This is a required field."));
        assertThat("Password error expected", createAccountPage.getFieldErrorText("password"), is("This is a required field."));
        assertThat("Confirm Password error expected", createAccountPage.getFieldErrorText("password-confirmation"), is("This is a required field."));
        assertThat("Should remain on Create Account page after submitting empty form", createAccountPage.getPageTitle(), is(equalToIgnoringCase("Create New Customer Account")));
        logger.info("Missing fields registration failure test completed.");
    }
}