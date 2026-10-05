package tests.customer;

import clients.AuthClient;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.TestCustomer;

import static core.Groups.*;

//GET /api/auth/verify is what the admin console's nginx calls to check a token and its scope
public class TokenVerificationTest {

    private TestCustomer customer;

    @BeforeClass(alwaysRun = true)
    public void createCustomer() {
        customer = CustomerFactory.create();
    }

    @Test(groups = {"CUS-VER-01", CUSTOMER, P1, REGRESSION}, description = "CUS-VER-01: valid token without scope gives 204 and X-Customer-Id")
    public void validTokenWithoutScope() {
        Response response = AuthClient.verify(customer.token(), null);

        Assert.assertEquals(response.statusCode(), 204);
        Assert.assertEquals(response.header("X-Customer-Id"), customer.id());
    }

    @Test(groups = {"CUS-VER-03", CUSTOMER, P1, REGRESSION}, description = "CUS-VER-03: customer token asking for orders:operate gives 403")
    public void customerTokenLacksOperatorScope() {
        Assert.assertEquals(AuthClient.verify(customer.token(), "orders:operate").statusCode(), 403);
    }

    @Test(groups = {"CUS-VER-04", CUSTOMER, P1, REGRESSION}, description = "CUS-VER-04: no token or a malformed token gives 401")
    public void missingOrMalformedTokenGives401() {
        Assert.assertEquals(AuthClient.verify(null, null).statusCode(), 401);
        Assert.assertEquals(AuthClient.verify("not-a-jwt", null).statusCode(), 401);
    }

    @Test(groups = {"CUS-VER-05", CUSTOMER, P2, REGRESSION}, description = "CUS-VER-05: a prefix of a scope (orders) is not accepted")
    public void scopePrefixIsNotEnough() {
        Assert.assertEquals(AuthClient.verify(customer.token(), "orders").statusCode(), 403);
    }

    @Test(groups = {"CUS-VER-06", CUSTOMER, P3, REGRESSION}, description = "CUS-VER-06: blank scope is treated as no scope")
    public void blankScopeIsIgnored() {
        Assert.assertEquals(AuthClient.verify(customer.token(), "").statusCode(), 204);
    }
}
