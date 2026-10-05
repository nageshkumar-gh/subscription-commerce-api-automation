package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

//One product as returned by GET /api/products. Fields the API adds later are ignored instead of failing
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        String storage,
        String finish,
        BigDecimal price,
        List<String> features,
        boolean active) {
}
