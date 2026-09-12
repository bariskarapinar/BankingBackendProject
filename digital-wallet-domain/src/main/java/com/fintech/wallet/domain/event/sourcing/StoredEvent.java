package com.fintech.wallet.domain.event.sourcing;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;
import java.time.Instant;

@Getter
public class StoredEvent {
    private final String eventId;
    private final String aggregateId;
    private final String aggregateType;
    private final long sequenceNumber;
    private final String eventType;
    private final String eventPayload;
    private final int eventVersion;
    private final Instant timestamp;
    private final String tenantId;

    public StoredEvent(String eventId, String aggregateId, String aggregateType, 
                      long sequenceNumber, String eventType, String eventPayload,
                      int eventVersion, Instant timestamp, String tenantId) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
        this.eventVersion = eventVersion;
        this.timestamp = timestamp;
        this.tenantId = tenantId;
    }

    public static StoredEvent from(DomainEvent event, String aggregateId, String aggregateType,
                                    long sequenceNumber, String eventPayload) {
        return from(event, aggregateId, aggregateType, sequenceNumber, eventPayload, "");
    }

    public static StoredEvent from(DomainEvent event, String aggregateId, String aggregateType,
                                   long sequenceNumber, String eventPayload, String tenantId) {
        return new StoredEvent(
                java.util.UUID.randomUUID().toString(),
                aggregateId,
                aggregateType,
                sequenceNumber,
                event.getClass().getSimpleName(),
                eventPayload,
                1,  // Event version (for upcasting)
                Instant.now(),
                tenantId
        );
    }
}
