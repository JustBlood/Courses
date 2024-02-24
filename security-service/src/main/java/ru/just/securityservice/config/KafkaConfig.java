package ru.just.securityservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Value("${topics.user-actions-topic}")
    private String userActionsTopic;

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name(userActionsTopic)
                .partitions(10)
                .build();
    }
}
