package com.fintech.wallet.infrastructure.repository;

import com.fintech.wallet.infrastructure.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import com.fintech.wallet.domain.transfer.TransferStatus;

@Repository
public interface TransferSpringDataRepository extends JpaRepository<TransferEntity, String> {
    
    @Query("SELECT t FROM TransferEntity t WHERE t.idempotencyKey = :idempotencyKey")
    Optional<TransferEntity> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    List<TransferEntity> findBySourceAccountIdOrderByCreatedAtDesc(String sourceAccountId);
    List<TransferEntity> findByDestinationAccountIdOrderByCreatedAtDesc(String destinationAccountId);
    List<TransferEntity> findByStatusOrderByCreatedAtDesc(TransferStatus status);
    List<TransferEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startDate, Instant endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransferEntity t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumAmountByCreatedAtBetween(@Param("startDate") Instant startDate,
                                                     @Param("endDate") Instant endDate);

    @Query("SELECT t.status, COUNT(t) FROM TransferEntity t GROUP BY t.status")
    List<Object[]> countByStatus();
}
