package tests.customer;

import clients.AuthClient;
import clients.CustomerClient;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.RegisterRequest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.JwtUtils;
import utilities.TestData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static core.Groups.*;
import static utilities.ApiAssertions.assertCustomerError;

public class CustomerRegistrationTest {

    private static final String NAME_REQUIRED = "Customer name is required";
    private static final String EMAIL_INVALID = "Email format is invalid";
    private static final String PHONE_INVALID = "Phone number must contain 7 to 15 digits";
    private static final String PASSWORD_INVALID = "Password must contain 8 to 72 characters";

    @Test(groups = {"CUS-REG-01", SMOKE, CUSTOMER, P1, REGRESSION}, description = "CUS-REG-01: register with valid data returns a token and an active customer")
    public void registerWithValidData() {
        RegisterRequest request = TestData.newRegisterRequest();

        Response response = register(request);

        Assert.assertEquals(response.statusCode(), 201);
        JsonPath body = response.jsonPath();
        Assert.assertFalse(body.getString("accessToken").isBlank());
        Assert.assertEquals(body.getString("tokenType"), "Bearer");
        Assert.assertEquals(body.getInt("expiresIn"), 3600);
        Assert.assertFalse(body.getString("customer.id").isBlank());
        Assert.assertTrue(body.getBoolean("customer.active"));
        Assert.assertEquals(body.getString("customer.name"), request.name());
        Assert.assertEquals(body.getString("customer.email"), request.email());
        Assert.assertEquals(body.getString("customer.phone"), request.phone());
    }

    @Test(groups = {"CUS-REG-02", CUSTOMER, P1, REGRESSION}, description = "CUS-REG-02: responses never expose the password or its hash")
    public void passwordIsNeverReturned() {
        Response registration = register(TestData.newRegisterRequest());
        Response profile = CustomerClient.getCustomer(registration.jsonPath().getString("accessToken"));

        Assert.assertFalse(registration.asString().toLowerCase().contains("password"), "Register response: " + registration.asString());
        Assert.assertFalse(profile.asString().toLowerCase().contains("password"), "Profile response: " + profile.asString());
    }

    @Test(groups = {"CUS-REG-03", CUSTOMER, P1, REGRESSION}, description = "CUS-REG-03: the same email cannot register twice")
    public void duplicateEmailIsRejected() {
        RegisterRequest request = TestData.newRegisterRequest();
        register(request);

        assertCustomerError(register(request), 409, "Customer with this email already exists");
    }

    @Test(groups = {"CUS-REG-04", CUSTOMER, P2, REGRESSION}, description = "CUS-REG-04: email uniqueness ignores case")
    public void duplicateEmailInOtherCaseIsRejected() {
        RegisterRequest request = TestData.newRegisterRequest();
        register(request);

        assertCustomerError(register(request.withEmail(request.email().toUpperCase())), 409, "already exists");
    }

    @Test(groups = {"CUS-REG-05", CUSTOMER, P2, REGRESSION}, description = "CUS-REG-05: email is trimmed and lower-cased")
    public void emailIsNormalised() {
        String email = TestData.uniqueEmail();

        Response response = register(TestData.newRegisterRequest().withEmail("  " + email.toUpperCase() + "  "));

        Assert.assertEquals(response.statusCode(), 201);
        Assert.assertEquals(response.jsonPath().getString("customer.email"), email);
    }

    @Test(groups = {"CUS-REG-06", CUSTOMER, P1, REGRESSION}, description = "CUS-REG-06: a registration token never carries a scope claim")
    public void registrationTokenHasNoScope() {
        Response response = register(TestData.newRegisterRequest());

        JsonPath claims = JwtUtils.claims(response.jsonPath().getString("accessToken"));
        Assert.assertNull(claims.get("scope"), "Unexpected scope: " + claims.get("scope"));
        Assert.assertEquals(claims.getString("iss"), "customer-service");
        Assert.assertEquals(claims.getString("sub"), response.jsonPath().getString("customer.id"));
    }

    @DataProvider
    public Object[][] invalidNames() {
        return new Object[][]{
                {"missing name", TestData.newRegisterRequest().withName(null)},
                {"blank name", TestData.newRegisterRequest().withName("   ")},
        };
    }

    @Test(groups = {"CUS-REG-07", CUSTOMER, P2, REGRESSION}, dataProvider = "invalidNames", description = "CUS-REG-07: missing or blank name gives 400")
    public void invalidNameIsRejected(String scenario, RegisterRequest request) {
        assertCustomerError(AuthClient.register(request), 400, NAME_REQUIRED);
    }

    @Test(groups = {"CUS-REG-08", CUSTOMER, P2, REGRESSION}, description = "CUS-REG-08: invalid email format gives 400")
    public void invalidEmailIsRejected() {
        assertCustomerError(AuthClient.register(TestData.newRegisterRequest().withEmail("not-an-email")), 400, EMAIL_INVALID);
    }

    @DataProvider
    public Object[][] invalidPhones() {
        return new Object[][]{
                {"phone with letters", "08712ab456"},
                {"phone 6 digits", "123456"},
                {"phone 16 digits", "1234567890123456"},
                {"phone with +", "+353871234567"},
        };
    }

    @Test(groups = {"CUS-REG-09", CUSTOMER, P2, REGRESSION}, dataProvider = "invalidPhones", description = "CUS-REG-09: phone with letters, 6 or 16 digits, or a + gives 400")
    public void invalidPhoneIsRejected(String scenario, String phone) {
        assertCustomerError(AuthClient.register(TestData.newRegisterRequest().withPhone(phone)), 400, PHONE_INVALID);
    }

    @DataProvider
    public Object[][] validPhoneBoundaries() {
        return new Object[][]{{"1234567"}, {"123456789012345"}};
    }

    @Test(groups = {"CUS-REG-10", CUSTOMER, P2, REGRESSION}, dataProvider = "validPhoneBoundaries", description = "CUS-REG-10: phones of exactly 7 and 15 digits are accepted")
    public void phoneBoundaryIsAccepted(String phone) {
        Response response = register(TestData.newRegisterRequest().withPhone(phone));

        Assert.assertEquals(response.statusCode(), 201, "phone " + phone + ": " + response.asString());
    }

    @DataProvider
    public Object[][] invalidPasswordLengths() {
        return new Object[][]{{7}, {73}};
    }

    @Test(groups = {"CUS-REG-11", CUSTOMER, P2, REGRESSION}, dataProvider = "invalidPasswordLengths", description = "CUS-REG-11: passwords of 7 or 73 characters give 400")
    public void invalidPasswordIsRejected(int length) {
        Response response = AuthClient.register(TestData.newRegisterRequest().withPassword(TestData.repeat('a', length)));

        assertCustomerError(response, 400, PASSWORD_INVALID);
    }

    @DataProvider
    public Object[][] validPasswordLengths() {
        return new Object[][]{{8}, {72}};
    }

    @Test(groups = {"CUS-REG-12", CUSTOMER, P3, REGRESSION}, dataProvider = "validPasswordLengths", description = "CUS-REG-12: passwords of exactly 8 and 72 characters are accepted")
    public void passwordBoundaryIsAccepted(int length) {
        Response response = register(TestData.newRegisterRequest().withPassword(TestData.repeat('a', length)));

        Assert.assertEquals(response.statusCode(), 201, "password length " + length + ": " + response.asString());
    }

    @Test(groups = {"CUS-REG-13", CUSTOMER, P3, REGRESSION}, description = "CUS-REG-13: unknown fields such as scope, active and id are ignored")
    public void extraFieldsGiveNoPrivilege() {
        RegisterRequest valid = TestData.newRegisterRequest();
        Map<String, Object> body = Map.of(
                "name", valid.name(), "email", valid.email(), "phone", valid.phone(), "password", valid.password(),
                "scope", "catalog:write orders:operate", "active", false, "id", "attacker-chosen-id");

        Response response = register(body);

        Assert.assertEquals(response.statusCode(), 201);
        Assert.assertNotEquals(response.jsonPath().getString("customer.id"), "attacker-chosen-id");
        Assert.assertTrue(response.jsonPath().getBoolean("customer.active"));
        Assert.assertNull(JwtUtils.claims(response.jsonPath().getString("accessToken")).get("scope"));
    }

    @Test(groups = {"CUS-REG-14", CUSTOMER, P3, REGRESSION}, description = "CUS-REG-14: concurrent registrations with one email create exactly one customer")
    public void concurrentRegistrationsCreateOneCustomer() throws Exception {
        RegisterRequest request = TestData.newRegisterRequest();
        int attempts = 5;

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Callable<Response>> calls = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            calls.add(() -> register(request));
        }
        List<Integer> statuses = new ArrayList<>();
        try {
            for (Future<Response> future : pool.invokeAll(calls)) {
                statuses.add(future.get().statusCode());
            }
        } finally {
            pool.shutdown();
        }

        Assert.assertEquals(statuses.stream().filter(s -> s == 201).count(), 1, "Statuses: " + statuses);
        Assert.assertEquals(statuses.stream().filter(s -> s == 409).count(), attempts - 1, "Statuses: " + statuses);
    }

    //Registers and marks the new customer for deletion at the end of the suite
    private static Response register(Object body) {
        Response response = AuthClient.register(body);
        if (response.statusCode() == 201) {
            CustomerFactory.trackForCleanup(response.jsonPath().getString("accessToken"));
        }
        return response;
    }
}
