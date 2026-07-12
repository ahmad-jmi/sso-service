package com.ahmad.sso.service.service;

import com.ahmad.sso.service.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// Publishes lightweight domain events - consumers (like user-service) fetch full
// detail via REST if needed, rather than carrying large/stale payloads on the bus.
@Service
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserCreated(UUID userId, UUID tenantId, String email) {
        publish("user.created", userId, tenantId, Map.of("email", email));
    }

    public void publishUserDeleted(UUID userId, UUID tenantId) {
        publish("user.deleted", userId, tenantId, Map.of());
    }

    public void publishPasswordReset(UUID userId, UUID tenantId) {
        publish("user.password_reset", userId, tenantId, Map.of());
    }

    private void publish(String eventType, UUID userId, UUID tenantId, Map<String, Object> extra) {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("event_type", eventType);
        event.put("user_id", userId.toString());
        event.put("tenant_id", tenantId.toString());
        event.put("timestamp", Instant.now().toString());
        event.putAll(extra);

        kafkaTemplate.send(KafkaConfig.USER_EVENTS_TOPIC, userId.toString(), event);
    }
}
