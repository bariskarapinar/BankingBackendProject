package com.fintech.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LedgerEntrySpringDataRepository extends JpaRepository<LedgerEntryJpaEntity, String> {
    List<LedgerEntryJpaEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);
    List<LedgerEntryJpaEntity> findByAccountIdAndTenantIdOrderByCreatedAtDesc(String accountId, String tenantId);
}
