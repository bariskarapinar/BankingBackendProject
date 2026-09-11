package com.fintech.wallet.infrastructure.persistence;

import com.fintech.wallet.domain.event.sourcing.Snapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "aggregate_snapshots", indexes = {
        @Index(name = "idx_snapshots_aggregate", columnList = "aggregate_id, sequence_number")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnapshotJpaEntity {
    @Id
    @Column(name = "snapshot_id", length = 36)
    private String snapshotId;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "aggregate_state", nullable = false, columnDefinition = "TEXT")
    private String aggregateState;

    @Column(name = "snapshot_version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant timestamp;

    public static SnapshotJpaEntity from(Snapshot snapshot) {
        SnapshotJpaEntity entity = new SnapshotJpaEntity();
        entity.snapshotId = snapshot.getSnapshotId();
        entity.aggregateId = snapshot.getAggregateId();
        entity.aggregateType = snapshot.getAggregateType();
        entity.sequenceNumber = snapshot.getSequenceNumber();
        entity.aggregateState = snapshot.getAggregateState();
        entity.version = snapshot.getVersion();
        entity.timestamp = snapshot.getTimestamp();
        return entity;
    }

    public Snapshot toDomain() {
        return new Snapshot(snapshotId, aggregateId, aggregateType, sequenceNumber,
                aggregateState, version, timestamp);
    }
}
