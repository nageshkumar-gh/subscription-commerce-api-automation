package tests.platform;

import clients.AuthClient;
import clients.GenericClient;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.TestCustomer;

import static core.Groups.*;
import static endpoints.Routes.MY_ORDERS;
import static endpoints.Routes.REGISTER;
import static utilities.ApiAssertions.assertStatusIn;

//Checks the design doc applies to every service (COM-xx), run here against customer and orchestration
public class CommonChecksTest {

    @Test(groups = {"COM-06", PLATFORM, P2, REGRESSION}, description = "COM-06: wrong HTTP method on a known path gives 405 (or 401/403 on secured services)")
    public void wrongMethodIsRejected() {
        assertStatusIn(GenericClient.send(Method.GET, REGISTER, null), 405, 401, 403);
    }

    @Test(groups = {"COM-07", PLATFORM, P2, REGRESSION}, description = "COM-07: malformed JSON body gives 400")
    public void malformedJsonIsRejected() {
        TestCustomer customer = CustomerFactory.create();

        assertStatusIn(AuthClient.registerWithRawBody("{\"name\":", ContentType.JSON), 400);
        assertStatusIn(GenericClient.sendRaw(Method.POST, MY_ORDERS, customer.token(), "{\"productId\":", ContentType.JSON), 400);
    }

    @Test(groups = {"COM-08", PLATFORM, P2, REGRESSION}, description = "COM-08: wrong Content-Type on a POST gives 415")
    public void wrongContentTypeIsRejected() {
        assertStatusIn(AuthClient.registerWithRawBody("name=QA", ContentType.TEXT), 415);
    }
}
