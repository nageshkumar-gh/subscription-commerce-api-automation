package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//One order lifecycle event: published to Kafka by orchestration-service, stored and served by tracking-service
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEventResponse(
        Integer schemaVersion,
        String eventId,
        String orderId,
        String customerId,
        String eventType,
        String status,
        String detail,
        String occurredAt) {
}
