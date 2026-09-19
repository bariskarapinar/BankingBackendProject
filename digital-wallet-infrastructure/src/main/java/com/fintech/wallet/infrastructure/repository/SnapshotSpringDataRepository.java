package com.fintech.wallet.infrastructure.repository;

import com.fintech.wallet.infrastructure.persistence.SnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotSpringDataRepository extends JpaRepository<SnapshotJpaEntity, String> {
    Optional<SnapshotJpaEntity> findFirstByAggregateIdOrderBySequenceNumberDesc(String aggregateId);
}
