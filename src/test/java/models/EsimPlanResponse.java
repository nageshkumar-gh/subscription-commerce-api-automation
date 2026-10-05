package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

//One eSIM plan as returned by GET /api/esim-plans
@JsonIgnoreProperties(ignoreUnknown = true)
public record EsimPlanResponse(
        String id,
        String code,
        String name,
        String description,
        BigDecimal monthlyPrice,
        boolean active) {
}
