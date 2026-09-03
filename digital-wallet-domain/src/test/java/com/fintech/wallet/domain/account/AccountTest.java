package com.fintech.wallet.domain.account;

import com.fintech.wallet.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Account Aggregate Tests")
class AccountTest {

    private AccountId accountId;
    private String tenantId;
    private String customerId;
    private Currency currency;
    private Money initialBalance;

    @BeforeEach
    void setUp() {
        accountId = AccountId.generate();
        tenantId = "tenant-1";
        customerId = "customer-1";
        currency = Currency.getInstance("USD");
        initialBalance = new Money(BigDecimal.valueOf(1000), currency);
    }

    @Test
    @DisplayName("Should create account successfully")
    void testCreateAccountSuccess() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);

        assertThat(account.getId()).isEqualTo(accountId);
        assertThat(account.getTenantId()).isEqualTo(tenantId);
        assertThat(account.getCustomerId()).isEqualTo(customerId);
        assertThat(account.getBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(account.getAvailableBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(account.getDomainEvents()).isNotEmpty();
    }

    @Test
    @DisplayName("Should deposit funds successfully")
    void testDepositFundsSuccess() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money depositAmount = new Money(BigDecimal.valueOf(500), currency);

        account.depositFunds(depositAmount);

        assertThat(account.getBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    @DisplayName("Should reject deposit with invalid amount")
    void testDepositFundsWithInvalidAmount() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);

        assertThatThrownBy(() -> account.depositFunds(null))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_DEPOSIT");
    }

    @Test
    @DisplayName("Should withdraw funds successfully")
    void testWithdrawFundsSuccess() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money withdrawAmount = new Money(BigDecimal.valueOf(300), currency);

        account.withdrawFunds(withdrawAmount);

        assertThat(account.getBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    @DisplayName("Should reject withdrawal when insufficient balance")
    void testWithdrawFundsInsufficientBalance() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money withdrawAmount = new Money(BigDecimal.valueOf(2000), currency);

        assertThatThrownBy(() -> account.withdrawFunds(withdrawAmount))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("INSUFFICIENT_BALANCE");
    }

    @Test
    @DisplayName("Should hold balance successfully")
    void testHoldBalanceSuccess() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money holdAmount = new Money(BigDecimal.valueOf(400), currency);

        account.holdBalance(holdAmount, "hold-ref-123");

        assertThat(account.getHoldBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(account.getAvailableBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    @DisplayName("Should reject hold when insufficient available balance")
    void testHoldBalanceInsufficientBalance() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money holdAmount = new Money(BigDecimal.valueOf(1500), currency);

        assertThatThrownBy(() -> account.holdBalance(holdAmount, "hold-ref"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("INSUFFICIENT_BALANCE_FOR_HOLD");
    }

    @Test
    @DisplayName("Should release held balance successfully")
    void testReleaseHoldSuccess() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money holdAmount = new Money(BigDecimal.valueOf(300), currency);

        account.holdBalance(holdAmount, "hold-ref-123");
        account.releaseHold(holdAmount, "hold-ref-123");

        assertThat(account.getHoldBalance().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getAvailableBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Should reject release when held balance is insufficient")
    void testReleaseHoldInsufficientHeldBalance() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);
        Money holdAmount = new Money(BigDecimal.valueOf(300), currency);
        Money releaseAmount = new Money(BigDecimal.valueOf(500), currency);

        account.holdBalance(holdAmount, "hold-ref-123");

        assertThatThrownBy(() -> account.releaseHold(releaseAmount, "hold-ref-123"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_RELEASE_AMOUNT");
    }

    @Test
    @DisplayName("Should track available balance correctly with holds")
    void testAvailableBalanceWithHolds() {
        Account account = new Account(accountId, tenantId, customerId, currency, initialBalance);

        account.holdBalance(new Money(BigDecimal.valueOf(200), currency), "hold-1");
        account.holdBalance(new Money(BigDecimal.valueOf(300), currency), "hold-2");

        assertThat(account.getBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(account.getHoldBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(account.getAvailableBalance().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }
}
