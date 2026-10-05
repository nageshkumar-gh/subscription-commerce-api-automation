package tests.customer;

import clients.AuthClient;
import clients.CustomerClient;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.LoginRequest;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.JwtUtils;
import utilities.TestCustomer;
import utilities.TestData;

import static core.Groups.*;
import static utilities.ApiAssertions.assertCustomerError;
import static utilities.ApiAssertions.assertStatusIn;

public class CustomerLoginTest {

    private static final String BAD_CREDENTIALS = "Email or password is incorrect";

    @Test(groups = {"CUS-LOG-01", SMOKE, CUSTOMER, P1, REGRESSION}, description = "CUS-LOG-01: valid credentials return a new token for the same customer")
    public void loginWithValidCredentials() {
        TestCustomer customer = CustomerFactory.create();

        Response response = AuthClient.login(new LoginRequest(customer.email(), customer.password()));

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("customer.id"), customer.id());
        Assert.assertEquals(JwtUtils.claims(response.jsonPath().getString("accessToken")).getString("sub"), customer.id());
    }

    @Test(groups = {"CUS-LOG-02", CUSTOMER, P1, REGRESSION}, description = "CUS-LOG-02: wrong password gives 401")
    public void wrongPasswordIsRejected() {
        TestCustomer customer = CustomerFactory.create();

        assertCustomerError(AuthClient.login(new LoginRequest(customer.email(), "WrongPassw0rd!")), 401, BAD_CREDENTIALS);
    }

    @Test(groups = {"CUS-LOG-03", CUSTOMER, P1, REGRESSION}, description = "CUS-LOG-03: unknown email gives the same 401 as a wrong password (no account enumeration)")
    public void unknownEmailLooksLikeWrongPassword() {
        assertCustomerError(AuthClient.login(new LoginRequest(TestData.uniqueEmail(), TestData.PASSWORD)), 401, BAD_CREDENTIALS);
    }

    @Test(groups = {"CUS-LOG-04", CUSTOMER, P2, REGRESSION}, description = "CUS-LOG-04: email in another case or with spaces still logs in")
    public void emailIsNormalisedOnLogin() {
        TestCustomer customer = CustomerFactory.create();

        Response response = AuthClient.login(new LoginRequest("  " + customer.email().toUpperCase() + " ", customer.password()));

        Assert.assertEquals(response.statusCode(), 200, response.asString());
    }

    @Test(groups = {"CUS-LOG-08", CUSTOMER, P2, REGRESSION}, description = "CUS-LOG-08: an ordinary customer's token has no scope claim")
    public void ordinaryCustomerHasNoScope() {
        TestCustomer customer = CustomerFactory.create();

        Response response = AuthClient.login(new LoginRequest(customer.email(), customer.password()));

        Assert.assertNull(JwtUtils.claims(response.jsonPath().getString("accessToken")).get("scope"));
    }

    @Test(groups = {"CUS-LOG-09", CUSTOMER, P2, REGRESSION}, description = "CUS-LOG-09: login fails after the customer deleted their profile")
    public void loginAfterDeleteIsRejected() {
        TestCustomer customer = CustomerFactory.create();
        Assert.assertEquals(CustomerClient.deleteCustomer(customer.token()).statusCode(), 204);

        assertCustomerError(AuthClient.login(new LoginRequest(customer.email(), customer.password())), 401, BAD_CREDENTIALS);
    }

    @Test(groups = {"CUS-LOG-10", CUSTOMER, P3, REGRESSION}, description = "CUS-LOG-10: blank password or invalid email format gives 400")
    public void invalidLoginRequestIsRejected() {
        assertStatusIn(AuthClient.login(new LoginRequest(TestData.uniqueEmail(), "")), 400);
        assertStatusIn(AuthClient.login(new LoginRequest("not-an-email", TestData.PASSWORD)), 400);
    }

    @Test(groups = {"CUS-LOG-11", CUSTOMER, P3, REGRESSION}, description = "CUS-LOG-11: token lifetime (exp - iat) is 3600 s and matches expiresIn")
    public void tokenLifetimeIsOneHour() {
        TestCustomer customer = CustomerFactory.create();

        Response response = AuthClient.login(new LoginRequest(customer.email(), customer.password()));

        JsonPath claims = JwtUtils.claims(response.jsonPath().getString("accessToken"));
        long lifetime = claims.getLong("exp") - claims.getLong("iat");
        Assert.assertEquals(lifetime, 3600L);
        Assert.assertEquals(response.jsonPath().getLong("expiresIn"), lifetime);
    }
}
