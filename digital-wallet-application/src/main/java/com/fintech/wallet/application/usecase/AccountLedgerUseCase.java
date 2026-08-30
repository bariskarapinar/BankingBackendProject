package com.fintech.wallet.application.usecase;

import com.fintech.wallet.application.command.CreateAccountCommand;
import com.fintech.wallet.application.command.DepositFundsCommand;
import com.fintech.wallet.application.command.HoldBalanceCommand;
import com.fintech.wallet.application.dto.AccountResponseDTO;
import com.fintech.wallet.application.event.OutboxEvent;
import com.fintech.wallet.application.port.AccountRepository;
import com.fintech.wallet.application.port.DistributedLockPort;
import com.fintech.wallet.application.port.OutboxEventRepository;
import com.fintech.wallet.common.exception.ApplicationException;
import com.fintech.wallet.common.exception.DomainException;
import com.fintech.wallet.common.exception.InfrastructureException;
import com.fintech.wallet.domain.account.Account;
import com.fintech.wallet.domain.account.AccountId;
import com.fintech.wallet.domain.account.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLedgerUseCase {
    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final DistributedLockPort distributedLockPort;
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    @Transactional
    public AccountResponseDTO createAccount(CreateAccountCommand command) {
        try {
            log.info("Creating account for tenant: {}, customer: {}", command.getTenantId(), command.getCustomerId());

            AccountId accountId = AccountId.generate();
            Currency currency = Currency.getInstance(command.getCurrency());
            Money initialBalance = new Money(new BigDecimal(command.getInitialBalance()), currency);

            Account account = new Account(accountId, command.getTenantId(), command.getCustomerId(), currency, initialBalance);

            Account savedAccount = accountRepository.save(account);

            persistDomainEvents(savedAccount.getDomainEvents());
            savedAccount.clearDomainEvents();

            return mapToResponseDTO(savedAccount);
        } catch (DomainException e) {
            log.error("Domain error creating account: {}", e.getErrorCode(), e);
            throw new ApplicationException(
                    "Failed to create account: " + e.getMessage(),
                    "CREATE_ACCOUNT_FAILED",
                    e.getDetails(),
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error creating account", e);
            throw new ApplicationException(
                    "Unexpected error creating account",
                    "CREATE_ACCOUNT_ERROR",
                    e.getMessage(),
                    e
            );
        }
    }

    @Transactional
    public AccountResponseDTO depositFunds(DepositFundsCommand command) {
        String lockKey = buildLockKey(command.getTenantId(), command.getAccountId());

        boolean lockAcquired = distributedLockPort.tryLock(lockKey, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!lockAcquired) {
            log.warn("Failed to acquire distributed lock for account: {}", command.getAccountId());
            throw new ApplicationException(
                    "Account is currently being modified",
                    "LOCK_ACQUISITION_FAILED",
                    "Could not acquire lock within timeout",
                    null
            );
        }

        try {
            return executeDepositWithLock(command);
        } finally {
            distributedLockPort.unlock(lockKey);
        }
    }

    private AccountResponseDTO executeDepositWithLock(DepositFundsCommand command) {
        try {
            AccountId accountId = new AccountId(command.getAccountId());

            Account account = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new ApplicationException(
                            "Account not found",
                            "ACCOUNT_NOT_FOUND",
                            "AccountId: " + command.getAccountId(),
                            null
                    ));

            Money depositAmount = new Money(new BigDecimal(command.getAmount()), account.getCurrency());
            account.depositFunds(depositAmount);

            Account savedAccount = accountRepository.save(account);

            persistDomainEvents(savedAccount.getDomainEvents());
            savedAccount.clearDomainEvents();

            log.info("Successfully deposited {} to account: {}", command.getAmount(), command.getAccountId());

            return mapToResponseDTO(savedAccount);
        } catch (DomainException e) {
            log.error("Domain error during deposit: {}", e.getErrorCode(), e);
            throw new ApplicationException(
                    "Failed to deposit funds: " + e.getMessage(),
                    "DEPOSIT_FAILED",
                    e.getDetails(),
                    e
            );
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during deposit", e);
            throw new ApplicationException(
                    "Unexpected error during deposit",
                    "DEPOSIT_ERROR",
                    e.getMessage(),
                    e
            );
        }
    }

    @Transactional
    public AccountResponseDTO holdBalance(HoldBalanceCommand command) {
        String lockKey = buildLockKey(command.getTenantId(), command.getAccountId());

        boolean lockAcquired = distributedLockPort.tryLock(lockKey, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!lockAcquired) {
            log.warn("Failed to acquire distributed lock for account: {}", command.getAccountId());
            throw new ApplicationException(
                    "Account is currently being modified",
                    "LOCK_ACQUISITION_FAILED",
                    "Could not acquire lock within timeout",
                    null
            );
        }

        try {
            return executeHoldWithLock(command);
        } finally {
            distributedLockPort.unlock(lockKey);
        }
    }

    private AccountResponseDTO executeHoldWithLock(HoldBalanceCommand command) {
        try {
            AccountId accountId = new AccountId(command.getAccountId());

            Account account = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new ApplicationException(
                            "Account not found",
                            "ACCOUNT_NOT_FOUND",
                            "AccountId: " + command.getAccountId(),
                            null
                    ));

            Money holdAmount = new Money(new BigDecimal(command.getAmount()), account.getCurrency());
            account.holdBalance(holdAmount, command.getHoldReference());

            Account savedAccount = accountRepository.save(account);

            persistDomainEvents(savedAccount.getDomainEvents());
            savedAccount.clearDomainEvents();

            log.info("Successfully held {} on account: {}", command.getAmount(), command.getAccountId());

            return mapToResponseDTO(savedAccount);
        } catch (DomainException e) {
            log.error("Domain error during hold: {}", e.getErrorCode(), e);
            throw new ApplicationException(
                    "Failed to hold balance: " + e.getMessage(),
                    "HOLD_FAILED",
                    e.getDetails(),
                    e
            );
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during hold", e);
            throw new ApplicationException(
                    "Unexpected error during hold",
                    "HOLD_ERROR",
                    e.getMessage(),
                    e
            );
        }
    }

    private void persistDomainEvents(List<com.fintech.wallet.common.model.DomainEvent> domainEvents) {
        try {
            for (com.fintech.wallet.common.model.DomainEvent event : domainEvents) {
                OutboxEvent outboxEvent = OutboxEvent.from(event);
                outboxEventRepository.save(outboxEvent);
                log.debug("Persisted domain event to outbox: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to persist domain events to outbox", e);
            throw new InfrastructureException(
                    "Failed to persist domain events",
                    "OUTBOX_PERSISTENCE_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    private String buildLockKey(String tenantId, String accountId) {
        return "account-lock:" + tenantId + ":" + accountId;
    }

    private AccountResponseDTO mapToResponseDTO(Account account) {
        return AccountResponseDTO.builder()
                .accountId(account.getId().getValue())
                .tenantId(account.getTenantId())
                .customerId(account.getCustomerId())
                .currency(account.getCurrency().getCurrencyCode())
                .balance(account.getBalance().getAmount().toPlainString())
                .holdBalance(account.getHoldBalance().getAmount().toPlainString())
                .availableBalance(account.getAvailableBalance().getAmount().toPlainString())
                .version(account.getVersion())
                .build();
    }
}
