package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.port.AccountRepository;
import com.fintech.wallet.common.exception.InfrastructureException;
import com.fintech.wallet.domain.account.Account;
import com.fintech.wallet.domain.account.AccountId;
import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.infrastructure.persistence.AccountJpaEntity;
import com.fintech.wallet.infrastructure.persistence.AccountSpringDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Currency;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository {
    private final AccountSpringDataRepository accountSpringDataRepository;

    @Override
    public Account save(Account account) {
        try {
            AccountJpaEntity entity = mapToJpaEntity(account);
            AccountJpaEntity savedEntity = accountSpringDataRepository.save(entity);
            log.debug("Account saved: {}", account.getId().getValue());
            return mapToDomainEntity(savedEntity);
        } catch (Exception e) {
            log.error("Failed to save account", e);
            throw new InfrastructureException(
                    "Failed to save account",
                    "ACCOUNT_SAVE_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {
        try {
            Optional<AccountJpaEntity> entity = accountSpringDataRepository.findById(accountId.getValue());
            return entity.map(this::mapToDomainEntity);
        } catch (Exception e) {
            log.error("Failed to find account by id: {}", accountId.getValue(), e);
            throw new InfrastructureException(
                    "Failed to find account",
                    "ACCOUNT_FIND_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public Optional<Account> findByIdWithLock(AccountId accountId) {
        try {
            Optional<AccountJpaEntity> entity = accountSpringDataRepository.findByIdWithPessimisticLock(accountId.getValue());
            return entity.map(this::mapToDomainEntity);
        } catch (Exception e) {
            log.error("Failed to find account with lock: {}", accountId.getValue(), e);
            throw new InfrastructureException(
                    "Failed to find account with lock",
                    "ACCOUNT_LOCK_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    private AccountJpaEntity mapToJpaEntity(Account account) {
        return AccountJpaEntity.builder()
                .id(account.getId().getValue())
                .tenantId(account.getTenantId())
                .customerId(account.getCustomerId())
                .currency(account.getCurrency().getCurrencyCode())
                .balance(account.getBalance().getAmount())
                .holdBalance(account.getHoldBalance().getAmount())
                .version(account.getVersion())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private Account mapToDomainEntity(AccountJpaEntity entity) {
        AccountId accountId = new AccountId(entity.getId());
        Currency currency = Currency.getInstance(entity.getCurrency());
        Money balance = new Money(entity.getBalance(), currency);

        Account account = new Account(
                accountId,
                entity.getTenantId(),
                entity.getCustomerId(),
                currency,
                balance
        );

        account.setVersion(entity.getVersion());
        account.setCreatedAt(entity.getCreatedAt());
        account.setUpdatedAt(entity.getUpdatedAt());

        Money holdBalance = new Money(entity.getHoldBalance(), currency);
        java.lang.reflect.Field field;
        try {
            field = Account.class.getDeclaredField("holdBalance");
            field.setAccessible(true);
            field.set(account, holdBalance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new InfrastructureException(
                    "Failed to set holdBalance via reflection",
                    "REFLECTION_ERROR",
                    e.getMessage(),
                    e
            );
        }

        return account;
    }
}
