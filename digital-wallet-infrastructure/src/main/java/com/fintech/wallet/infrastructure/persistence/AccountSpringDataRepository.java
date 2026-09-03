package com.fintech.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountSpringDataRepository extends JpaRepository<AccountJpaEntity, String> {
    Optional<AccountJpaEntity> findByIdAndTenantId(String id, String tenantId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id = ?1")
    Optional<AccountJpaEntity> findByIdWithOptimisticLock(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id = ?1")
    Optional<AccountJpaEntity> findByIdWithPessimisticLock(String id);
}
