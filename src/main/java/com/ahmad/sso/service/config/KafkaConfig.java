package com.ahmad.sso.service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    public static final String USER_EVENTS_TOPIC = "user-events";

    // Auto-creates the topic on startup in local/dev environments.
    // In staging/prod, topics are typically provisioned separately with proper
    // partitioning/replication - remove this bean or gate it behind a profile.
//    @Bean
//    public NewTopic userEventsTopic() {
//        return new NewTopic(USER_EVENTS_TOPIC, 3, (short) 1);
//    }
}
