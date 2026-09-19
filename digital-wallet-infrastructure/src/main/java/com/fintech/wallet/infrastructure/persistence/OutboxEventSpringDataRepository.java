package com.fintech.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxEventSpringDataRepository extends JpaRepository<OutboxEventJpaEntity, String> {
    @Query("SELECT e FROM OutboxEventJpaEntity e WHERE e.published = false ORDER BY e.createdAt ASC LIMIT ?1")
    List<OutboxEventJpaEntity> findUnpublishedEvents(int limit);

    List<OutboxEventJpaEntity> findByPublishedFalseOrderByCreatedAtAsc();
}
