package clients;

import core.BaseClient;
import io.restassured.response.Response;

import static endpoints.Routes.MY_ORDER;
import static endpoints.Routes.MY_ORDERS;
import static endpoints.Routes.MY_ORDER_CANCEL;
import static endpoints.Routes.MY_ORDER_CHECKOUT;
import static endpoints.Routes.MY_ORDER_EVENTS;
import static endpoints.Routes.MY_SUBSCRIPTIONS;

//Storefront API of orchestration-service: the orders of the customer who owns the token
public class StorefrontClient extends BaseClient {

    public static Response placeOrder(String token, Object placeOrderBody) {
        return authorizedRequest(token)
                .body(placeOrderBody)
                .when()
                .post(MY_ORDERS);
    }

    public static Response getOrders(String token) {
        return authorizedRequest(token)
                .when()
                .get(MY_ORDERS);
    }

    public static Response getOrder(String token, String orderId) {
        return authorizedRequest(token)
                .pathParam("orderId", orderId)
                .when()
                .get(MY_ORDER);
    }

    public static Response checkout(String token, String orderId) {
        return authorizedRequest(token)
                .pathParam("orderId", orderId)
                .when()
                .post(MY_ORDER_CHECKOUT);
    }

    public static Response cancelOrder(String token, String orderId, Object cancelBody) {
        return authorizedRequest(token)
                .pathParam("orderId", orderId)
                .body(cancelBody)
                .when()
                .post(MY_ORDER_CANCEL);
    }

    public static Response getOrderEvents(String token, String orderId) {
        return authorizedRequest(token)
                .pathParam("orderId", orderId)
                .when()
                .get(MY_ORDER_EVENTS);
    }

    public static Response getSubscriptions(String token) {
        return authorizedRequest(token)
                .when()
                .get(MY_SUBSCRIPTIONS);
    }
}
