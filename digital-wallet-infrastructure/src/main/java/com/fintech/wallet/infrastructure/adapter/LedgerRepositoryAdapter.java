package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.port.LedgerRepository;
import com.fintech.wallet.common.exception.InfrastructureException;
import com.fintech.wallet.domain.ledger.LedgerEntry;
import com.fintech.wallet.infrastructure.persistence.LedgerEntryJpaEntity;
import com.fintech.wallet.infrastructure.persistence.LedgerEntrySpringDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepository {
    private final LedgerEntrySpringDataRepository ledgerEntrySpringDataRepository;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        try {
            LedgerEntryJpaEntity entity = mapToJpaEntity(entry);
            LedgerEntryJpaEntity savedEntity = ledgerEntrySpringDataRepository.save(entity);
            log.debug("Ledger entry saved for account: {}", entry.getAccountId());
            return mapToDomainEntity(savedEntity);
        } catch (Exception e) {
            log.error("Failed to save ledger entry", e);
            throw new InfrastructureException(
                    "Failed to save ledger entry",
                    "LEDGER_SAVE_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<LedgerEntry> findByAccountId(String accountId) {
        try {
            List<LedgerEntryJpaEntity> entities = ledgerEntrySpringDataRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
            return entities.stream()
                    .map(this::mapToDomainEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find ledger entries for account: {}", accountId, e);
            throw new InfrastructureException(
                    "Failed to find ledger entries",
                    "LEDGER_FIND_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    private LedgerEntryJpaEntity mapToJpaEntity(LedgerEntry entry) {
        return LedgerEntryJpaEntity.builder()
                .id(entry.getId())
                .accountId(entry.getAccountId())
                .tenantId(entry.getTenantId())
                .type(LedgerEntryJpaEntity.LedgerEntryType.valueOf(entry.getType().name()))
                .amount(entry.getAmount())
                .currency(entry.getCurrency().getCurrencyCode())
                .reference(entry.getReference())
                .description(entry.getDescription())
                .build();
    }

    private LedgerEntry mapToDomainEntity(LedgerEntryJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        LedgerEntry.LedgerEntryType type = LedgerEntry.LedgerEntryType.valueOf(entity.getType().name());

        LedgerEntry entry = new LedgerEntry(
                entity.getId(),
                entity.getAccountId(),
                entity.getTenantId(),
                type,
                entity.getAmount(),
                currency,
                entity.getReference(),
                entity.getDescription()
        );

        java.lang.reflect.Field field;
        try {
            field = LedgerEntry.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entry, entity.getCreatedAt());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new InfrastructureException(
                    "Failed to set createdAt via reflection",
                    "REFLECTION_ERROR",
                    e.getMessage(),
                    e
            );
        }

        return entry;
    }
}
