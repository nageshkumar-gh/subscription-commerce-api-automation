package utilities;

import clients.StorefrontClient;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.CancelOrderRequest;
import models.OrderResponse;
import models.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.time.Duration;
import java.util.function.Predicate;

//Reusable storefront steps, so order tests read as a sequence of business actions
public final class OrderSteps {

    private static final Logger LOG = LoggerFactory.getLogger(OrderSteps.class);
    public static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(60);

    private OrderSteps() {
    }

    //Places an order for the first active phone and plan in the catalogue
    public static OrderResponse placeOrder(TestCustomer customer) {
        PlaceOrderRequest request = new PlaceOrderRequest(CatalogueData.firstProduct().id(), CatalogueData.firstPlan().id());
        Response response = StorefrontClient.placeOrder(customer.token(), request);
        Assert.assertEquals(response.statusCode(), 201, "Place order failed: " + response.asString());
        OrderResponse order = response.as(OrderResponse.class);
        LOG.debug("Placed order {} for customer {}", order.id(), customer.id());
        return order;
    }

    public static void checkout(TestCustomer customer, String orderId) {
        Response response = StorefrontClient.checkout(customer.token(), orderId);
        Assert.assertEquals(response.statusCode(), 202, "Checkout failed: " + response.asString());
        LOG.debug("Checked out order {}", orderId);
    }

    //Polls the order details (order, payment, delivery, activation, billing, events) until the condition holds
    public static JsonPath waitForOrderDetails(TestCustomer customer, String orderId, String description, Predicate<JsonPath> condition) {
        return Poller.waitUntil(description + " for order " + orderId,
                () -> StorefrontClient.getOrder(customer.token(), orderId).jsonPath(),
                condition,
                ASYNC_TIMEOUT);
    }

    //Clean-up: voids the pending payment of a checked-out order. Errors are ignored (it may already be cancelled)
    public static void cancelQuietly(TestCustomer customer, String orderId) {
        StorefrontClient.cancelOrder(customer.token(), orderId, new CancelOrderRequest("Automation clean-up"));
    }
}
