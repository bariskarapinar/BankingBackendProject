package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.port.EventStorePort;
import com.fintech.wallet.domain.event.sourcing.Snapshot;
import com.fintech.wallet.domain.event.sourcing.StoredEvent;
import com.fintech.wallet.infrastructure.persistence.SnapshotJpaEntity;
import com.fintech.wallet.infrastructure.persistence.StoredEventJpaEntity;
import com.fintech.wallet.infrastructure.repository.SnapshotSpringDataRepository;
import com.fintech.wallet.infrastructure.repository.StoredEventSpringDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventStoreRepositoryAdapter implements EventStorePort {
    private final StoredEventSpringDataRepository eventRepository;
    private final SnapshotSpringDataRepository snapshotRepository;

    @Override
    @Transactional
    public void appendEvent(StoredEvent event) {
        eventRepository.save(StoredEventJpaEntity.from(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsForAggregate(String aggregateId) {
        return eventRepository.findByAggregateIdOrderBySequenceNumberAsc(aggregateId)
                .stream().map(StoredEventJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsSince(String aggregateId, long sequenceNumber) {
        return eventRepository.findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        aggregateId, sequenceNumber)
                .stream().map(StoredEventJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsByType(String eventType) {
        return eventRepository.findByEventTypeOrderBySequenceNumberAsc(eventType)
                .stream().map(StoredEventJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsByTenant(String tenantId) {
        return eventRepository.findByTenantIdOrderByTimestampAsc(tenantId)
                .stream().map(StoredEventJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveSnapshot(Snapshot snapshot) {
        snapshotRepository.save(SnapshotJpaEntity.from(snapshot));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Snapshot> getLatestSnapshot(String aggregateId) {
        return snapshotRepository.findFirstByAggregateIdOrderBySequenceNumberDesc(aggregateId)
                .map(SnapshotJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long getEventCount() {
        return eventRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getEventCount(String aggregateId) {
        return eventRepository.countByAggregateId(aggregateId);
    }
}
