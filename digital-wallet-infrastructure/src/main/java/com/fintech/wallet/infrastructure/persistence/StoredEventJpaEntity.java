package com.fintech.wallet.infrastructure.persistence;

import com.fintech.wallet.domain.event.sourcing.StoredEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "stored_events", indexes = {
        @Index(name = "idx_stored_events_aggregate", columnList = "aggregate_id, sequence_number", unique = true),
        @Index(name = "idx_stored_events_type", columnList = "event_type"),
        @Index(name = "idx_stored_events_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredEventJpaEntity {
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
    private String eventPayload;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    public static StoredEventJpaEntity from(StoredEvent event) {
        StoredEventJpaEntity entity = new StoredEventJpaEntity();
        entity.eventId = event.getEventId();
        entity.aggregateId = event.getAggregateId();
        entity.aggregateType = event.getAggregateType();
        entity.sequenceNumber = event.getSequenceNumber();
        entity.eventType = event.getEventType();
        entity.eventPayload = event.getEventPayload();
        entity.eventVersion = event.getEventVersion();
        entity.timestamp = event.getTimestamp();
        entity.tenantId = event.getTenantId();
        return entity;
    }

    public StoredEvent toDomain() {
        return new StoredEvent(eventId, aggregateId, aggregateType, sequenceNumber,
                eventType, eventPayload, eventVersion, timestamp, tenantId);
    }
}
