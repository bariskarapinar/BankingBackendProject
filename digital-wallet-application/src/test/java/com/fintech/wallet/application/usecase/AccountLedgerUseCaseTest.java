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
import com.fintech.wallet.domain.account.Account;
import com.fintech.wallet.domain.account.AccountId;
import com.fintech.wallet.domain.account.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLedgerUseCase Tests")
class AccountLedgerUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private DistributedLockPort distributedLockPort;

    private AccountLedgerUseCase accountLedgerUseCase;

    @BeforeEach
    void setUp() {
        accountLedgerUseCase = new AccountLedgerUseCase(
                accountRepository,
                outboxEventRepository,
                distributedLockPort
        );
    }

    @Test
    @DisplayName("Should create account successfully with initial balance")
    void testCreateAccountSuccess() {
        CreateAccountCommand command = new CreateAccountCommand(
                "tenant-1",
                "customer-1",
                "USD",
                "1000.00"
        );

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            return account;
        });

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        AccountResponseDTO response = accountLedgerUseCase.createAccount(command);

        assertThat(response).isNotNull();
        assertThat(response.getTenantId()).isEqualTo("tenant-1");
        assertThat(response.getCustomerId()).isEqualTo("customer-1");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getBalance()).isEqualTo("1000.00");
        assertThat(response.getAvailableBalance()).isEqualTo("1000.00");

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should deposit funds successfully with distributed lock")
    void testDepositFundsSuccess() {
        String accountId = "account-1";
        String tenantId = "tenant-1";

        DepositFundsCommand command = new DepositFundsCommand(accountId, "500.00", tenantId);

        Account mockAccount = new Account(
                new AccountId(accountId),
                tenantId,
                "customer-1",
                Currency.getInstance("USD"),
                new Money(BigDecimal.valueOf(1000), Currency.getInstance("USD"))
        );

        when(distributedLockPort.tryLock(anyString(), eq(5L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        when(accountRepository.findByIdWithLock(new AccountId(accountId)))
                .thenReturn(Optional.of(mockAccount));

        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        AccountResponseDTO response = accountLedgerUseCase.depositFunds(command);

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo("1500.00");

        verify(distributedLockPort, times(1)).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(distributedLockPort, times(1)).unlock(anyString());
        verify(accountRepository, times(1)).findByIdWithLock(any(AccountId.class));
    }

    @Test
    @DisplayName("Should fail deposit when lock cannot be acquired")
    void testDepositFundsLockAcquisitionFailure() {
        String accountId = "account-1";
        String tenantId = "tenant-1";

        DepositFundsCommand command = new DepositFundsCommand(accountId, "500.00", tenantId);

        when(distributedLockPort.tryLock(anyString(), eq(5L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        assertThatThrownBy(() -> accountLedgerUseCase.depositFunds(command))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Account is currently being modified")
                .extracting("errorCode")
                .isEqualTo("LOCK_ACQUISITION_FAILED");

        verify(distributedLockPort, times(1)).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(distributedLockPort, never()).unlock(anyString());
    }

    @Test
    @DisplayName("Should hold balance successfully with distributed lock")
    void testHoldBalanceSuccess() {
        String accountId = "account-1";
        String tenantId = "tenant-1";

        HoldBalanceCommand command = new HoldBalanceCommand(
                accountId,
                "200.00",
                "hold-ref-123",
                tenantId
        );

        Account mockAccount = new Account(
                new AccountId(accountId),
                tenantId,
                "customer-1",
                Currency.getInstance("USD"),
                new Money(BigDecimal.valueOf(1000), Currency.getInstance("USD"))
        );

        when(distributedLockPort.tryLock(anyString(), eq(5L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        when(accountRepository.findByIdWithLock(new AccountId(accountId)))
                .thenReturn(Optional.of(mockAccount));

        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        AccountResponseDTO response = accountLedgerUseCase.holdBalance(command);

        assertThat(response).isNotNull();
        assertThat(response.getHoldBalance()).isEqualTo("200.00");
        assertThat(response.getAvailableBalance()).isEqualTo("800.00");

        verify(distributedLockPort, times(1)).tryLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(distributedLockPort, times(1)).unlock(anyString());
    }

    @Test
    @DisplayName("Should fail deposit when account not found")
    void testDepositFundsAccountNotFound() {
        String accountId = "non-existent";
        String tenantId = "tenant-1";

        DepositFundsCommand command = new DepositFundsCommand(accountId, "500.00", tenantId);

        when(distributedLockPort.tryLock(anyString(), eq(5L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        when(accountRepository.findByIdWithLock(any(AccountId.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountLedgerUseCase.depositFunds(command))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Account not found")
                .extracting("errorCode")
                .isEqualTo("ACCOUNT_NOT_FOUND");

        verify(distributedLockPort, times(1)).unlock(anyString());
    }
}
