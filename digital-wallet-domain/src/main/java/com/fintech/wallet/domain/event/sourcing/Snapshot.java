package com.fintech.wallet.domain.event.sourcing;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;
import java.time.Instant;

@Getter
public class Snapshot {
    private final String snapshotId;
    private final String aggregateId;
    private final String aggregateType;
    private final long sequenceNumber;
    private final String aggregateState;
    private final int version;
    private final Instant timestamp;

    public Snapshot(String snapshotId, String aggregateId, String aggregateType,
                   long sequenceNumber, String aggregateState, int version, Instant timestamp) {
        this.snapshotId = snapshotId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.sequenceNumber = sequenceNumber;
        this.aggregateState = aggregateState;
        this.version = version;
        this.timestamp = timestamp;
    }

    public static Snapshot create(String aggregateId, String aggregateType, 
                                  long sequenceNumber, String aggregateState, int version) {
        return new Snapshot(
                java.util.UUID.randomUUID().toString(),
                aggregateId,
                aggregateType,
                sequenceNumber,
                aggregateState,
                version,
                Instant.now()
        );
    }

    public boolean isStaleFor(long currentSequenceNumber) {
        // Snapshot is stale if more than 100 events since snapshot
        return currentSequenceNumber - sequenceNumber > 100;
    }
}
