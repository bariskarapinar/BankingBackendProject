package com.fintech.wallet.application.event;

import com.fintech.wallet.common.model.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class OutboxEvent {
    private final String id;
    private final String eventType;
    private final String aggregateId;
    private final String payload;
    
    @Setter(AccessLevel.PACKAGE)
    private boolean published;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant publishedAt;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;

    public OutboxEvent(DomainEvent domainEvent, String payload) {
        this.id = UUID.randomUUID().toString();
        this.eventType = domainEvent.getEventType();
        this.aggregateId = domainEvent.getAggregateId();
        this.payload = payload;
        this.published = false;
        this.createdAt = Instant.now();
    }

    public static OutboxEvent from(DomainEvent event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String payload = mapper.writeValueAsString(event);
            return new OutboxEvent(event, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize domain event", e);
        }
    }

    protected OutboxEvent() {
        this.id = null;
        this.eventType = null;
        this.aggregateId = null;
        this.payload = null;
    }
}
