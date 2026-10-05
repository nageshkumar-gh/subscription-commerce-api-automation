package tests.orchestration;

import clients.StorefrontClient;
import io.restassured.response.Response;
import models.CancelOrderRequest;
import models.OrderEventResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utilities.CustomerFactory;
import utilities.OrderSteps;
import utilities.Poller;
import utilities.TestCustomer;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static core.Groups.*;

//Kafka pipeline checks through the APIs: orchestration-service publishes order-lifecycle-events,
//tracking-service consumes them, and the storefront serves them back at /api/me/orders/{id}/events
public class OrderLifecycleEventsTest {

    private static final Set<String> EVENT_TYPES =
            Set.of("ORDER_WORKFLOW", "PAYMENT", "FULFILLMENT", "ESIM_ACTIVATION", "BILLING");
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(60);

    private TestCustomer customer;
    private String customerId;
    private String orderId;
    private List<OrderEventResponse> events;

    //One order is placed and checked out for the whole class; the tests then inspect its events
    @BeforeClass(alwaysRun = true)
    public void placeAndCheckOutOrder() {
        customer = CustomerFactory.create();
        customerId = customer.id();
        orderId = OrderSteps.placeOrder(customer).id();
        OrderSteps.checkout(customer, orderId);

        events = waitForEvents(orderId, list ->
                hasEvent(list, "ORDER_WORKFLOW", "WAITING_FOR_PAYMENT") && hasEvent(list, "PAYMENT", "PENDING"));
    }

    //Cancelling voids the pending payment, so test orders do not pile up waiting for an operator
    @AfterClass(alwaysRun = true)
    public void cancelOrder() {
        if (orderId != null) {
            OrderSteps.cancelQuietly(customer, orderId);
        }
    }

    @Test(groups = {"ORC-STO-14", SMOKE, ORCHESTRATION, TRACKING, KAFKA, P1, REGRESSION}, description = "ORC-STO-14: checkout publishes STARTED, WAITING_FOR_PAYMENT and PAYMENT PENDING")
    public void checkoutPublishesInitialLifecycleEvents() {
        Assert.assertTrue(hasEvent(events, "ORDER_WORKFLOW", "STARTED"), "Missing ORDER_WORKFLOW STARTED in " + events);
        Assert.assertTrue(hasEvent(events, "ORDER_WORKFLOW", "WAITING_FOR_PAYMENT"));
        Assert.assertTrue(hasEvent(events, "PAYMENT", "PENDING"));
    }

    @Test(groups = {"ORC-EVT-01", "ORC-EVT-02", ORCHESTRATION, TRACKING, KAFKA, P1, REGRESSION}, description = "ORC-EVT-01, ORC-EVT-02: every event has the contract fields and a known event type")
    public void everyEventFollowsTheContract() {
        for (OrderEventResponse event : events) {
            if (event.schemaVersion() != null) {
                Assert.assertEquals(event.schemaVersion().intValue(), 1, "schemaVersion of " + event.eventId());
            }
            Assert.assertFalse(isBlank(event.eventId()), "Event without eventId: " + event);
            Assert.assertEquals(event.orderId(), orderId, "orderId of " + event.eventId());
            Assert.assertEquals(event.customerId(), customerId, "customerId of " + event.eventId());
            Assert.assertTrue(EVENT_TYPES.contains(event.eventType()), "Unknown eventType: " + event.eventType());
            Assert.assertFalse(isBlank(event.status()), "Event without status: " + event.eventId());
            Assert.assertFalse(isBlank(event.occurredAt()), "Event without occurredAt: " + event.eventId());
        }
    }

    @Test(groups = {"ORC-EVT-03", ORCHESTRATION, TRACKING, KAFKA, P2, REGRESSION}, description = "ORC-EVT-03: eventId is order-<orderId>-<seq>-<type>-<status> with a strictly increasing seq")
    public void eventIdsCarryOrderAndIncreasingSequence() {
        long previousSequence = -1;
        for (OrderEventResponse event : events) {
            Pattern format = Pattern.compile("order-" + Pattern.quote(orderId) + "-(\\d+)-"
                    + Pattern.quote(event.eventType()) + "-" + Pattern.quote(event.status()));
            Matcher matcher = format.matcher(event.eventId());
            Assert.assertTrue(matcher.matches(), "Unexpected eventId format: " + event.eventId());

            long sequence = Long.parseLong(matcher.group(1));
            Assert.assertTrue(sequence > previousSequence,
                    "Sequence not increasing at " + event.eventId() + " (previous " + previousSequence + ")");
            previousSequence = sequence;
        }
    }

    @Test(groups = {"ORC-EVT-05", ORCHESTRATION, TRACKING, KAFKA, P2, REGRESSION}, description = "ORC-EVT-05: each status change is published once")
    public void eachStatusIsPublishedOnce() {
        Set<String> seen = new HashSet<>();
        for (OrderEventResponse event : events) {
            String typeAndStatus = event.eventType() + " " + event.status();
            Assert.assertTrue(seen.add(typeAndStatus), "Duplicate event: " + typeAndStatus);
        }
    }

    @Test(groups = {"TRK-API-01", ORCHESTRATION, TRACKING, KAFKA, P1, REGRESSION}, description = "TRK-API-01: events are returned oldest first")
    public void eventsAreReturnedOldestFirst() {
        for (int i = 1; i < events.size(); i++) {
            String previous = events.get(i - 1).occurredAt();
            String current = events.get(i).occurredAt();
            Assert.assertTrue(Instant.parse(previous).compareTo(Instant.parse(current)) <= 0,
                    "Events out of order: " + previous + " then " + current);
        }
    }

    @Test(groups = {"ORC-WF-09", ORCHESTRATION, TRACKING, KAFKA, P1, REGRESSION}, description = "ORC-WF-09: cancelling before checkout publishes exactly one CANCELLED event")
    public void cancelBeforeCheckoutPublishesOneCancelledEvent() {
        String uncheckedOrderId = OrderSteps.placeOrder(customer).id();
        String reason = "Changed my mind " + System.currentTimeMillis();

        Response cancel = StorefrontClient.cancelOrder(customer.token(), uncheckedOrderId, new CancelOrderRequest(reason));
        Assert.assertEquals(cancel.statusCode(), 202);
        Assert.assertEquals(cancel.jsonPath().getString("status"), "CANCELLED");

        List<OrderEventResponse> cancelEvents = waitForEvents(uncheckedOrderId,
                list -> hasEvent(list, "ORDER_WORKFLOW", "CANCELLED"));

        List<OrderEventResponse> cancelled = cancelEvents.stream()
                .filter(e -> e.eventType().equals("ORDER_WORKFLOW") && e.status().equals("CANCELLED"))
                .toList();
        Assert.assertEquals(cancelled.size(), 1, "Expected one CANCELLED event but got " + cancelEvents);
        Assert.assertTrue(cancelled.get(0).eventId().matches("order-" + Pattern.quote(uncheckedOrderId) + "-cancel-\\d+"),
                "Unexpected eventId: " + cancelled.get(0).eventId());
        Assert.assertTrue(cancelled.get(0).detail() != null && cancelled.get(0).detail().contains(reason),
                "Cancel reason missing from detail: " + cancelled.get(0).detail());
    }

    //Events travel through Kafka asynchronously, so poll the events API until the expected ones arrive
    private List<OrderEventResponse> waitForEvents(String orderId, Predicate<List<OrderEventResponse>> condition) {
        return Poller.waitUntil("lifecycle events of order " + orderId,
                () -> List.of(StorefrontClient.getOrderEvents(customer.token(), orderId).as(OrderEventResponse[].class)),
                condition,
                EVENT_TIMEOUT);
    }

    private static boolean hasEvent(List<OrderEventResponse> events, String eventType, String status) {
        return events.stream().anyMatch(e -> eventType.equals(e.eventType()) && status.equals(e.status()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
