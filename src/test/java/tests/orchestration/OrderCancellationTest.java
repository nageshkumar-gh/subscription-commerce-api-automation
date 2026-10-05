package tests.orchestration;

import clients.StorefrontClient;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.CancelOrderRequest;
import models.OrderEventResponse;
import models.OrderResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.OrderSteps;
import utilities.TestCustomer;
import utilities.TestData;

import java.util.List;
import java.util.Map;

import static core.Groups.*;
import static utilities.ApiAssertions.assertStorefrontError;

public class OrderCancellationTest {

    @Test(groups = {"ORC-STO-15", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-15: cancelling an order that was never checked out is immediately CANCELLED")
    public void cancelBeforeCheckout() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);

        Response response = StorefrontClient.cancelOrder(customer.token(), order.id(), new CancelOrderRequest("Not needed"));

        Assert.assertEquals(response.statusCode(), 202, response.asString());
        Assert.assertEquals(response.jsonPath().getString("status"), "CANCELLED");
        Assert.assertEquals(StorefrontClient.getOrder(customer.token(), order.id()).jsonPath().getString("order.status"), "CANCELLED");
    }

    @Test(groups = {"ORC-STO-15", "ORC-CMP-01", "ORC-CMP-05", ORCHESTRATION, TRACKING, KAFKA, P1, REGRESSION}, description = "ORC-STO-15, ORC-CMP-01, ORC-CMP-05: cancel while waiting for payment voids the payment and publishes CANCELLING then CANCELLED")
    public void cancelWhileWaitingForPayment() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);
        OrderSteps.checkout(customer, order.id());
        OrderSteps.waitForOrderDetails(customer, order.id(), "payment to be created", details -> details.get("payment") != null);
        String reason = "Found a better deal " + TestData.unique("reason");

        Response cancel = StorefrontClient.cancelOrder(customer.token(), order.id(), new CancelOrderRequest(reason));

        Assert.assertEquals(cancel.statusCode(), 202, cancel.asString());
        Assert.assertEquals(cancel.jsonPath().getString("status"), "CANCELLING");

        JsonPath details = OrderSteps.waitForOrderDetails(customer, order.id(), "order CANCELLED and payment FAILED",
                d -> "CANCELLED".equals(d.getString("order.status")) && "FAILED".equals(d.getString("payment.status"))
                        && hasEvent(d, "ORDER_WORKFLOW", "CANCELLED"));

        Assert.assertTrue(String.valueOf(details.getString("payment.statusReason")).contains("payment voided"),
                "Payment reason: " + details.getString("payment.statusReason"));

        List<OrderEventResponse> events = details.getList("events", OrderEventResponse.class);
        OrderEventResponse secondLast = events.get(events.size() - 2);
        OrderEventResponse last = events.get(events.size() - 1);
        Assert.assertEquals(secondLast.eventType() + " " + secondLast.status(), "ORDER_WORKFLOW CANCELLING", "Events: " + events);
        Assert.assertEquals(last.eventType() + " " + last.status(), "ORDER_WORKFLOW CANCELLED", "Events: " + events);
        Assert.assertTrue(last.detail() != null && last.detail().startsWith("Customer: " + reason),
                "CANCELLED detail should start with 'Customer: <reason>': " + last.detail());
    }

    @Test(groups = {"ORC-STO-17", ORCHESTRATION, P2, REGRESSION}, description = "ORC-STO-17: cancel without a reason or with more than 300 characters gives 400")
    public void invalidCancelReasonIsRejected() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);

        Response noBody = StorefrontClient.cancelOrder(customer.token(), order.id(), Map.of());
        Response blank = StorefrontClient.cancelOrder(customer.token(), order.id(), new CancelOrderRequest("  "));
        Response tooLong = StorefrontClient.cancelOrder(customer.token(), order.id(), new CancelOrderRequest(TestData.repeat('r', 301)));

        Assert.assertEquals(noBody.statusCode(), 400, noBody.asString());
        Assert.assertEquals(blank.statusCode(), 400, blank.asString());
        Assert.assertEquals(tooLong.statusCode(), 400, tooLong.asString());
    }

    @Test(groups = {"ORC-STO-18", ORCHESTRATION, P2, REGRESSION}, description = "ORC-STO-18: checking out a cancelled order gives 409 'Order is cancelled'")
    public void checkoutCancelledOrderIsRejected() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);
        StorefrontClient.cancelOrder(customer.token(), order.id(), new CancelOrderRequest("Not needed"));

        assertStorefrontError(StorefrontClient.checkout(customer.token(), order.id()), 409, "Order is cancelled");
    }

    private static boolean hasEvent(JsonPath details, String eventType, String status) {
        List<Map<String, Object>> events = details.getList("events");
        return events != null && events.stream()
                .anyMatch(e -> eventType.equals(e.get("eventType")) && status.equals(e.get("status")));
    }
}
