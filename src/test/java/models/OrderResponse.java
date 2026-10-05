package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

//An order as returned by the storefront (POST/GET /api/me/orders)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderResponse(
        String id,
        String customerId,
        String productId,
        String planId,
        String productName,
        String planName,
        String storage,
        BigDecimal devicePrice,
        BigDecimal monthlyPrice,
        BigDecimal total,
        String status,
        String createdAt,
        String updatedAt) {
}
