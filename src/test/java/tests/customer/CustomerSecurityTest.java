package tests.customer;

import clients.AuthClient;
import clients.CustomerClient;
import clients.GenericClient;
import io.restassured.http.Method;
import io.restassured.response.Response;
import models.LoginRequest;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.JwtUtils;
import utilities.TestCustomer;

import static core.Groups.*;
import static endpoints.Routes.CUSTOMERS;
import static endpoints.Routes.PROFILE;
import static utilities.ApiAssertions.assertStatusIn;

public class CustomerSecurityTest {

    @Test(groups = {"CUS-SEC-01", CUSTOMER, P1, REGRESSION}, description = "CUS-SEC-01: /api/customers/me without a token gives 401")
    public void profileWithoutTokenGives401() {
        Assert.assertEquals(CustomerClient.getCustomer(null).statusCode(), 401);
    }

    @Test(groups = {"CUS-SEC-03", CUSTOMER, P1, REGRESSION}, description = "CUS-SEC-03: token signed with a different secret gives 401")
    public void wrongSecretTokenGives401() {
        TestCustomer customer = CustomerFactory.create();

        Assert.assertEquals(CustomerClient.getCustomer(JwtUtils.signedWithWrongSecret(customer.token())).statusCode(), 401);
    }

    @Test(groups = {"CUS-SEC-05", CUSTOMER, P2, REGRESSION}, description = "CUS-SEC-05: token with alg none gives 401")
    public void algNoneTokenGives401() {
        TestCustomer customer = CustomerFactory.create();

        Assert.assertEquals(CustomerClient.getCustomer(JwtUtils.withAlgNone(customer.token())).statusCode(), 401);
    }

    @Test(groups = {"CUS-SEC-06", CUSTOMER, P2, REGRESSION}, description = "CUS-SEC-06: token with a tampered subject gives 401")
    public void tamperedSubjectGives401() {
        TestCustomer attacker = CustomerFactory.create();
        TestCustomer victim = CustomerFactory.create();

        Response response = CustomerClient.getCustomer(JwtUtils.withSubject(attacker.token(), victim.id()));

        Assert.assertEquals(response.statusCode(), 401, "Tampered token was accepted: " + response.asString());
    }

    @Test(groups = {"CUS-SEC-07", CUSTOMER, P2, REGRESSION}, description = "CUS-SEC-07: no endpoint lists or fetches other customers")
    public void otherCustomersCannotBeListedOrFetched() {
        TestCustomer customer = CustomerFactory.create();
        TestCustomer other = CustomerFactory.create();

        for (String path : new String[]{CUSTOMERS, CUSTOMERS + "/" + other.id()}) {
            Response response = GenericClient.send(Method.GET, path, customer.token());
            assertStatusIn(response, 401, 403, 404, 405);
            Assert.assertFalse(response.asString().contains(other.email()), path + " leaked another customer's data");
        }
    }

    @Test(groups = {"CUS-SEC-08", CUSTOMER, P3, REGRESSION}, description = "CUS-SEC-08: CORS preflight from a non-allowed origin gets no Access-Control-Allow-Origin")
    public void corsRejectsUnknownOrigin() {
        Response response = GenericClient.preflight(PROFILE, "https://evil.example.test", "GET");

        Assert.assertNull(response.header("Access-Control-Allow-Origin"),
                "Unexpected CORS header for an unknown origin: " + response.header("Access-Control-Allow-Origin"));
    }

    @Test(groups = {"CUS-SEC-09", CUSTOMER, P3, REGRESSION}, description = "CUS-SEC-09: known gap, no lockout after many wrong passwords")
    public void noLockoutAfterWrongPasswords() {
        TestCustomer customer = CustomerFactory.create();
        for (int i = 0; i < 10; i++) {
            AuthClient.login(new LoginRequest(customer.email(), "WrongPassw0rd!" + i));
        }

        //Documents current behaviour: the right password still works. If lockout is added, update this test
        Assert.assertEquals(AuthClient.login(new LoginRequest(customer.email(), customer.password())).statusCode(), 200);
    }
}
