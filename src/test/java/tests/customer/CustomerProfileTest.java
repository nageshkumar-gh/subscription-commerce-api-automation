package tests.customer;

import clients.AuthClient;
import clients.CustomerClient;
import io.restassured.response.Response;
import models.CustomerRequest;
import models.LoginRequest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.JwtUtils;
import utilities.TestCustomer;
import utilities.TestData;

import static core.Groups.*;
import static utilities.ApiAssertions.assertCustomerError;

public class CustomerProfileTest {

    @Test(groups = {"CUS-ME-01", SMOKE, CUSTOMER, P1, REGRESSION}, description = "CUS-ME-01: GET /me returns the profile of the token's subject")
    public void getOwnProfile() {
        TestCustomer customer = CustomerFactory.create();

        Response response = CustomerClient.getCustomer(customer.token());

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("id"), JwtUtils.claims(customer.token()).getString("sub"));
        Assert.assertEquals(response.jsonPath().getString("email"), customer.email());
        Assert.assertEquals(response.jsonPath().getString("name"), customer.name());
    }

    @Test(groups = {"CUS-ME-02", CUSTOMER, P1, REGRESSION}, description = "CUS-ME-02: PUT /me replaces name, email and phone, and the change is persisted")
    public void updateOwnProfile() {
        TestCustomer customer = CustomerFactory.create();
        CustomerRequest update = profile("Updated " + TestData.unique("name"), TestData.uniqueEmail(), "0861112222");

        Response response = CustomerClient.updateCustomer(customer.token(), update);

        Assert.assertEquals(response.statusCode(), 200, response.asString());
        Response reloaded = CustomerClient.getCustomer(customer.token());
        Assert.assertEquals(reloaded.jsonPath().getString("name"), update.getName());
        Assert.assertEquals(reloaded.jsonPath().getString("email"), update.getEmail());
        Assert.assertEquals(reloaded.jsonPath().getString("phone"), update.getPhone());
    }

    @Test(groups = {"CUS-ME-03", CUSTOMER, P1, REGRESSION}, description = "CUS-ME-03: cannot take an email owned by another customer")
    public void emailOfAnotherCustomerIsRejected() {
        TestCustomer customer = CustomerFactory.create();
        TestCustomer other = CustomerFactory.create();

        Response response = CustomerClient.updateCustomer(customer.token(), profile(customer.name(), other.email(), customer.phone()));

        assertCustomerError(response, 409, "already exists");
    }

    @Test(groups = {"CUS-ME-04", CUSTOMER, P2, REGRESSION}, description = "CUS-ME-04: keeping your own email (in any case) is not a conflict")
    public void keepingOwnEmailIsAllowed() {
        TestCustomer customer = CustomerFactory.create();

        Response response = CustomerClient.updateCustomer(customer.token(),
                profile(customer.name(), customer.email().toUpperCase(), customer.phone()));

        Assert.assertEquals(response.statusCode(), 200, response.asString());
    }

    @DataProvider
    public Object[][] invalidProfiles() {
        return new Object[][]{
                {"blank name", "  ", TestData.uniqueEmail(), TestData.PHONE, "Customer name is required"},
                {"invalid email", "QA", "not-an-email", TestData.PHONE, "Email format is invalid"},
                {"invalid phone", "QA", TestData.uniqueEmail(), "12ab", "Phone number must contain 7 to 15 digits"},
        };
    }

    @Test(groups = {"CUS-ME-05", CUSTOMER, P2, REGRESSION}, dataProvider = "invalidProfiles", description = "CUS-ME-05: invalid profile fields give the same messages as registration")
    public void invalidProfileIsRejected(String scenario, String name, String email, String phone, String expectedMessage) {
        TestCustomer customer = CustomerFactory.create();

        assertCustomerError(CustomerClient.updateCustomer(customer.token(), profile(name, email, phone)), 400, expectedMessage);
    }

    @Test(groups = {"CUS-ME-06", CUSTOMER, P2, REGRESSION}, description = "CUS-ME-06: after changing email, the new email logs in and the old one does not")
    public void loginFollowsEmailChange() {
        TestCustomer customer = CustomerFactory.create();
        String newEmail = TestData.uniqueEmail();
        CustomerClient.updateCustomer(customer.token(), profile(customer.name(), newEmail, customer.phone()));

        Assert.assertEquals(AuthClient.login(new LoginRequest(newEmail, customer.password())).statusCode(), 200);
        Assert.assertEquals(AuthClient.login(new LoginRequest(customer.email(), customer.password())).statusCode(), 401);
    }

    @Test(groups = {"CUS-ME-07", CUSTOMER, P2, REGRESSION}, description = "CUS-ME-07: PUT /me does not change the password")
    public void passwordUnchangedByUpdate() {
        TestCustomer customer = CustomerFactory.create();
        CustomerClient.updateCustomer(customer.token(), profile("Renamed", customer.email(), customer.phone()));

        Assert.assertEquals(AuthClient.login(new LoginRequest(customer.email(), customer.password())).statusCode(), 200);
    }

    @Test(groups = {"CUS-ME-08", CUSTOMER, P1, REGRESSION}, description = "CUS-ME-08: DELETE /me returns 204, then the same token gets 404")
    public void deleteOwnProfile() {
        TestCustomer customer = CustomerFactory.create();

        Assert.assertEquals(CustomerClient.deleteCustomer(customer.token()).statusCode(), 204);

        assertCustomerError(CustomerClient.getCustomer(customer.token()), 404, "Customer not found");
    }

    @Test(groups = {"CUS-ME-09", CUSTOMER, P2, REGRESSION}, description = "CUS-ME-09: deleting twice gives 404 the second time")
    public void deleteTwiceGives404() {
        TestCustomer customer = CustomerFactory.create();
        CustomerClient.deleteCustomer(customer.token());

        Assert.assertEquals(CustomerClient.deleteCustomer(customer.token()).statusCode(), 404);
    }

    private static CustomerRequest profile(String name, String email, String phone) {
        CustomerRequest request = new CustomerRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPhone(phone);
        return request;
    }
}
