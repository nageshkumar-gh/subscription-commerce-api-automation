package tests.orchestration;

import clients.StorefrontClient;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.EsimPlanResponse;
import models.OrderResponse;
import models.PlaceOrderRequest;
import models.ProductResponse;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.CatalogueData;
import utilities.CustomerFactory;
import utilities.OrderSteps;
import utilities.TestCustomer;
import utilities.TestData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static core.Groups.*;
import static utilities.ApiAssertions.assertStorefrontError;

public class StorefrontOrderTest {

    @Test(groups = {"ORC-STO-01", "ORC-STO-02", SMOKE, ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-01, ORC-STO-02: place an order at catalogue prices; total = phone + plan")
    public void placeOrderAtCataloguePrices() {
        TestCustomer customer = CustomerFactory.create();
        ProductResponse product = CatalogueData.firstProduct();
        EsimPlanResponse plan = CatalogueData.firstPlan();

        Response response = StorefrontClient.placeOrder(customer.token(), new PlaceOrderRequest(product.id(), plan.id()));

        Assert.assertEquals(response.statusCode(), 201, response.asString());
        OrderResponse order = response.as(OrderResponse.class);
        Assert.assertEquals(order.customerId(), customer.id());
        Assert.assertEquals(order.status(), "PENDING_PAYMENT");
        Assert.assertEquals(order.productId(), product.id());
        Assert.assertEquals(order.planId(), plan.id());
        Assert.assertEquals(order.productName(), product.name());
        Assert.assertEquals(order.planName(), plan.name());
        assertAmount(order.devicePrice(), product.price(), "devicePrice");
        assertAmount(order.monthlyPrice(), plan.monthlyPrice(), "monthlyPrice");
        assertAmount(order.total(), product.price().add(plan.monthlyPrice()), "total");
    }

    @Test(groups = {"ORC-STO-03", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-03: customerId, devicePrice and total sent by the client are ignored")
    public void clientSuppliedPricesAreIgnored() {
        TestCustomer customer = CustomerFactory.create();
        ProductResponse product = CatalogueData.firstProduct();
        EsimPlanResponse plan = CatalogueData.firstPlan();
        Map<String, Object> body = Map.of(
                "productId", product.id(), "planId", plan.id(),
                "customerId", "someone-else", "devicePrice", 1, "monthlyPrice", 1, "total", 2);

        OrderResponse order = StorefrontClient.placeOrder(customer.token(), body).as(OrderResponse.class);

        Assert.assertEquals(order.customerId(), customer.id());
        assertAmount(order.devicePrice(), product.price(), "devicePrice");
        assertAmount(order.total(), product.price().add(plan.monthlyPrice()), "total");
    }

    @Test(groups = {"ORC-STO-04", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-04: unknown product gives 400 'That phone is not available'")
    public void unknownProductIsRejected() {
        TestCustomer customer = CustomerFactory.create();

        Response response = StorefrontClient.placeOrder(customer.token(),
                new PlaceOrderRequest("000000000000000000000000", CatalogueData.firstPlan().id()));

        assertStorefrontError(response, 400, "That phone is not available");
    }

    @Test(groups = {"ORC-STO-05", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-05: unknown plan gives 400 'That plan is not available'")
    public void unknownPlanIsRejected() {
        TestCustomer customer = CustomerFactory.create();

        Response response = StorefrontClient.placeOrder(customer.token(),
                new PlaceOrderRequest(CatalogueData.firstProduct().id(), "000000000000000000000000"));

        assertStorefrontError(response, 400, "That plan is not available");
    }

    @DataProvider
    public Object[][] invalidOrders() {
        return new Object[][]{
                {"missing productId", new PlaceOrderRequest(null, "plan-1")},
                {"blank planId", new PlaceOrderRequest("product-1", "  ")},
                {"productId over 100 chars", new PlaceOrderRequest(TestData.repeat('p', 101), "plan-1")},
        };
    }

    @Test(groups = {"ORC-STO-06", ORCHESTRATION, P2, REGRESSION}, dataProvider = "invalidOrders", description = "ORC-STO-06: missing, blank or too-long ids give 400")
    public void invalidOrderIsRejected(String scenario, PlaceOrderRequest request) {
        TestCustomer customer = CustomerFactory.create();

        Response response = StorefrontClient.placeOrder(customer.token(), request);

        Assert.assertEquals(response.statusCode(), 400, scenario + ": " + response.asString());
    }

    @Test(groups = {"ORC-STO-10", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-10: my orders list contains only my orders, newest first")
    public void listMyOrdersNewestFirst() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse first = OrderSteps.placeOrder(customer);
        OrderResponse second = OrderSteps.placeOrder(customer);

        List<OrderResponse> orders = List.of(StorefrontClient.getOrders(customer.token()).as(OrderResponse[].class));

        Assert.assertEquals(orders.stream().map(OrderResponse::id).toList(), List.of(second.id(), first.id()));
    }

    @Test(groups = {"ORC-STO-11", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-11: details right after placing: no step records yet and no events")
    public void detailsBeforeCheckoutHaveNoStepRecords() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);

        Response response = StorefrontClient.getOrder(customer.token(), order.id());

        Assert.assertEquals(response.statusCode(), 200);
        Map<String, Object> details = response.jsonPath().getMap("$");
        Assert.assertEquals(response.jsonPath().getString("order.id"), order.id());
        Assert.assertEquals(response.jsonPath().getList("events"), List.of());
        //Every record other than the order and its events (payment, delivery, activation, billing) is still null
        details.forEach((key, value) -> {
            if (!key.equals("order") && !key.equals("events")) {
                Assert.assertNull(value, key + " should be null before checkout");
            }
        });
    }

    @Test(groups = {"ORC-STO-12", SMOKE, ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-12: checkout starts the saga with 202 STARTED")
    public void checkoutStartsWorkflow() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);
        try {
            Response response = StorefrontClient.checkout(customer.token(), order.id());

            Assert.assertEquals(response.statusCode(), 202, response.asString());
            Assert.assertEquals(response.jsonPath().getString("orderId"), order.id());
            Assert.assertEquals(response.jsonPath().getString("status"), "STARTED");
        } finally {
            OrderSteps.cancelQuietly(customer, order.id());
        }
    }

    @Test(groups = {"ORC-STO-13", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-13: checking out again is safe and creates no second payment")
    public void checkoutAgainIsRepeatSafe() {
        TestCustomer customer = CustomerFactory.create();
        OrderResponse order = OrderSteps.placeOrder(customer);
        try {
            OrderSteps.checkout(customer, order.id());
            JsonPath before = OrderSteps.waitForOrderDetails(customer, order.id(), "payment to be created",
                    details -> details.get("payment") != null);

            Response again = StorefrontClient.checkout(customer.token(), order.id());

            Assert.assertEquals(again.statusCode(), 202, again.asString());
            Assert.assertEquals(again.jsonPath().getString("orderId"), order.id());
            JsonPath after = StorefrontClient.getOrder(customer.token(), order.id()).jsonPath();
            Assert.assertEquals(after.getString("payment.id"), before.getString("payment.id"), "A second payment was created");
            Assert.assertEquals(after.getString("payment.status"), "PENDING");
        } finally {
            OrderSteps.cancelQuietly(customer, order.id());
        }
    }

    @Test(groups = {"ORC-STO-19", ORCHESTRATION, P1, REGRESSION}, description = "ORC-STO-19: a new customer has no active subscriptions")
    public void newCustomerHasNoSubscriptions() {
        TestCustomer customer = CustomerFactory.create();

        Response response = StorefrontClient.getSubscriptions(customer.token());

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getList("$"), List.of());
    }

    private static void assertAmount(BigDecimal actual, BigDecimal expected, String field) {
        Assert.assertNotNull(actual, field + " is missing");
        Assert.assertEquals(actual.compareTo(expected), 0, field + ": expected " + expected + " but got " + actual);
    }
}
