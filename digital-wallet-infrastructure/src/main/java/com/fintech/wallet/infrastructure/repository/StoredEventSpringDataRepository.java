package com.fintech.wallet.infrastructure.repository;

import com.fintech.wallet.infrastructure.persistence.StoredEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredEventSpringDataRepository extends JpaRepository<StoredEventJpaEntity, String> {
    List<StoredEventJpaEntity> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);
    List<StoredEventJpaEntity> findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            String aggregateId, long sequenceNumber);
    List<StoredEventJpaEntity> findByEventTypeOrderBySequenceNumberAsc(String eventType);
    List<StoredEventJpaEntity> findByTenantIdOrderByTimestampAsc(String tenantId);
    long countByAggregateId(String aggregateId);
}
