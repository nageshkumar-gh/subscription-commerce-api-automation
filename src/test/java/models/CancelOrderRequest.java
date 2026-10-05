package models;

//Body of POST /api/me/orders/{orderId}/cancel (reason up to 300 characters)
public record CancelOrderRequest(String reason) {
}
