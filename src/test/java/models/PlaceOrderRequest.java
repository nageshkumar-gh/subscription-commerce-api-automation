package models;

//Body of POST /api/me/orders. Prices and customer come from the server, never from the request
public record PlaceOrderRequest(String productId, String planId) {
}
