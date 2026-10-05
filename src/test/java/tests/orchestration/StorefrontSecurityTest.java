package tests.orchestration;

import clients.StorefrontClient;
import io.restassured.response.Response;
import models.CancelOrderRequest;
import models.OrderResponse;
import models.PlaceOrderRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.JwtUtils;
import utilities.OrderSteps;
import utilities.TestCustomer;

import java.util.function.Function;

import static core.Groups.*;
import static utilities.ApiAssertions.assertStorefrontError;

public class StorefrontSecurityTest {

    private TestCustomer customerA;
    private TestCustomer customerB;
    private OrderResponse orderOfA;

    @BeforeClass(alwaysRun = true)
    public void createCustomersAndOrder() {
        customerA = CustomerFactory.create();
        customerB = CustomerFactory.create();
        orderOfA = OrderSteps.placeOrder(customerA);
    }

    @DataProvider
    public Object[][] storefrontCalls() {
        String orderId = "000000000000000000000000";
        return new Object[][]{
                {"place order", (Function<String, Response>) t -> StorefrontClient.placeOrder(t, new PlaceOrderRequest("p", "p"))},
                {"list orders", (Function<String, Response>) StorefrontClient::getOrders},
                {"get order", (Function<String, Response>) t -> StorefrontClient.getOrder(t, orderId)},
                {"checkout", (Function<String, Response>) t -> StorefrontClient.checkout(t, orderId)},
                {"cancel", (Function<String, Response>) t -> StorefrontClient.cancelOrder(t, orderId, new CancelOrderRequest("x"))},
                {"events", (Function<String, Response>) t -> StorefrontClient.getOrderEvents(t, orderId)},
                {"subscriptions", (Function<String, Response>) StorefrontClient::getSubscriptions},
        };
    }

    @Test(groups = {"ORC-SEC-01", ORCHESTRATION, P1, REGRESSION}, dataProvider = "storefrontCalls", description = "ORC-SEC-01: every /api/me call without a token gives 401")
    public void callWithoutTokenGives401(String call, Function<String, Response> request) {
        Assert.assertEquals(request.apply(null).statusCode(), 401, call);
    }

    @Test(groups = {"ORC-SEC-02", ORCHESTRATION, P1, REGRESSION}, description = "ORC-SEC-02: customer B gets 404 'Order not found' for A's order (read, events, checkout, cancel)")
    public void otherCustomersOrderIsInvisible() {
        String token = customerB.token();
        String orderId = orderOfA.id();

        assertStorefrontError(StorefrontClient.getOrder(token, orderId), 404, "Order not found");
        assertStorefrontError(StorefrontClient.getOrderEvents(token, orderId), 404, "Order not found");
        assertStorefrontError(StorefrontClient.checkout(token, orderId), 404, "Order not found");
        assertStorefrontError(StorefrontClient.cancelOrder(token, orderId, new CancelOrderRequest("x")), 404, "Order not found");
        //A's order must be untouched by B's attempts
        Assert.assertEquals(StorefrontClient.getOrder(customerA.token(), orderId).jsonPath().getString("order.status"), "PENDING_PAYMENT");
    }

    @Test(groups = {"ORC-SEC-03", ORCHESTRATION, P1, REGRESSION}, description = "ORC-SEC-03: customer B's order list and subscriptions never contain A's data")
    public void otherCustomersListsAreSeparate() {
        Assert.assertFalse(StorefrontClient.getOrders(customerB.token()).asString().contains(orderOfA.id()));
        Assert.assertFalse(StorefrontClient.getSubscriptions(customerB.token()).asString().contains(customerA.id()));
    }

    @Test(groups = {"ORC-SEC-04", ORCHESTRATION, P2, REGRESSION}, description = "ORC-SEC-04: forged tokens (wrong secret, alg none, tampered subject) give 401")
    public void forgedTokensGive401() {
        String token = customerB.token();

        Assert.assertEquals(StorefrontClient.getOrders(JwtUtils.signedWithWrongSecret(token)).statusCode(), 401);
        Assert.assertEquals(StorefrontClient.getOrders(JwtUtils.withAlgNone(token)).statusCode(), 401);
        Assert.assertEquals(StorefrontClient.getOrders(JwtUtils.withSubject(token, customerA.id())).statusCode(), 401);
    }
}
