package com.fintech.wallet.infrastructure.event;

import com.fintech.wallet.application.port.EventPublisherPort;
import com.fintech.wallet.common.model.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisherPort {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        try {
            String eventType = event.getEventType();
            String topic = buildTopicName(eventType);
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, event.getAggregateId(), message)
                    .thenAccept(result -> log.info(
                            "Event published to Kafka: topic={}, eventType={}, aggregateId={}",
                            topic, eventType, event.getAggregateId()
                    ))
                    .exceptionally(ex -> {
                        log.error("Failed to publish event to Kafka: {}", eventType, ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error serializing domain event", e);
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }

    private String buildTopicName(String eventType) {
        return "wallet." + eventType.replaceAll("(?<!^)(?=[A-Z])", "-").toLowerCase();
    }
}
