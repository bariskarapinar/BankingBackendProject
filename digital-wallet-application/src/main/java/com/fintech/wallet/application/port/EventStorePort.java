package com.fintech.wallet.application.port;

import com.fintech.wallet.domain.event.sourcing.StoredEvent;
import com.fintech.wallet.domain.event.sourcing.Snapshot;

import java.util.List;
import java.util.Optional;

public interface EventStorePort {
    
    /**
     * Append event to event log (immutable append-only)
     */
    void appendEvent(StoredEvent event);

    /**
     * Get all events for an aggregate (for replay)
     */
    List<StoredEvent> getEventsForAggregate(String aggregateId);

    /**
     * Get events for aggregate since sequence number (for incremental replay)
     */
    List<StoredEvent> getEventsSince(String aggregateId, long sequenceNumber);

    /**
     * Get all events of a specific type (for event listeners)
     */
    List<StoredEvent> getEventsByType(String eventType);

    /**
     * Get events by tenant (for multi-tenant auditing)
     */
    List<StoredEvent> getEventsByTenant(String tenantId);

    /**
     * Save snapshot (optimization for large event logs)
     */
    void saveSnapshot(Snapshot snapshot);

    /**
     * Get latest snapshot for aggregate
     */
    Optional<Snapshot> getLatestSnapshot(String aggregateId);

    /**
     * Get total event count (for monitoring)
     */
    long getEventCount();

    /**
     * Get event count for aggregate
     */
    long getEventCount(String aggregateId);
}
