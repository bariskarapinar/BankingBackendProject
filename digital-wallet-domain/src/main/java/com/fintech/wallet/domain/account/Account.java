package com.fintech.wallet.domain.account;

import com.fintech.wallet.common.exception.DomainException;
import com.fintech.wallet.common.model.DomainEvent;
import com.fintech.wallet.domain.event.AccountCreatedEvent;
import com.fintech.wallet.domain.event.BalanceHeldEvent;
import com.fintech.wallet.domain.event.BalanceReleasedEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Currency;

@Getter
public class Account {
    private final AccountId id;
    private final String tenantId;
    private final String customerId;
    private final Currency currency;
    private Money balance;
    private Money holdBalance;
    
    @Setter(AccessLevel.PACKAGE)
    private long version;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Account(AccountId id, String tenantId, String customerId, Currency currency, Money initialBalance) {
        if (id == null || tenantId == null || customerId == null || currency == null || initialBalance == null) {
            throw new DomainException(
                    "Account fields cannot be null",
                    "INVALID_ACCOUNT_CREATION",
                    ""
            );
        }
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.currency = currency;
        this.balance = initialBalance;
        this.holdBalance = new Money(java.math.BigDecimal.ZERO, currency);
        this.version = 1;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        this.domainEvents.add(new AccountCreatedEvent(
                id.getValue(),
                tenantId,
                customerId,
                currency.getCurrencyCode(),
                initialBalance.getAmount().toPlainString()
        ));
    }

    public void depositFunds(Money amount) {
        if (amount == null || !amount.getCurrency().equals(this.currency)) {
            throw new DomainException(
                    "Invalid deposit amount",
                    "INVALID_DEPOSIT",
                    "Currency mismatch or null amount"
            );
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void withdrawFunds(Money amount) {
        if (amount == null || !amount.getCurrency().equals(this.currency)) {
            throw new DomainException(
                    "Invalid withdrawal amount",
                    "INVALID_WITHDRAWAL",
                    "Currency mismatch or null amount"
            );
        }
        Money availableBalance = this.balance.subtract(this.holdBalance);
        if (!availableBalance.isGreaterThanOrEqual(amount)) {
            throw new DomainException(
                    "Insufficient available balance",
                    "INSUFFICIENT_BALANCE",
                    "Available: " + availableBalance.getAmount() + ", Required: " + amount.getAmount()
            );
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void holdBalance(Money amount, String holdReference) {
        if (amount == null || !amount.getCurrency().equals(this.currency)) {
            throw new DomainException(
                    "Invalid hold amount",
                    "INVALID_HOLD",
                    "Currency mismatch or null amount"
            );
        }
        Money availableBalance = this.balance.subtract(this.holdBalance);
        if (!availableBalance.isGreaterThanOrEqual(amount)) {
            throw new DomainException(
                    "Insufficient available balance for hold",
                    "INSUFFICIENT_BALANCE_FOR_HOLD",
                    "Available: " + availableBalance.getAmount() + ", Required: " + amount.getAmount()
            );
        }
        this.holdBalance = this.holdBalance.add(amount);
        this.updatedAt = Instant.now();
        
        this.domainEvents.add(new BalanceHeldEvent(
                this.id.getValue(),
                amount.getAmount().toPlainString(),
                holdReference
        ));
    }

    public void releaseHold(Money amount, String holdReference) {
        if (amount == null || !amount.getCurrency().equals(this.currency)) {
            throw new DomainException(
                    "Invalid release amount",
                    "INVALID_RELEASE",
                    "Currency mismatch or null amount"
            );
        }
        if (!this.holdBalance.isGreaterThanOrEqual(amount)) {
            throw new DomainException(
                    "Cannot release more than held balance",
                    "INVALID_RELEASE_AMOUNT",
                    "Held: " + holdBalance.getAmount() + ", Requesting: " + amount.getAmount()
            );
        }
        this.holdBalance = this.holdBalance.subtract(amount);
        this.updatedAt = Instant.now();
        
        this.domainEvents.add(new BalanceReleasedEvent(
                this.id.getValue(),
                amount.getAmount().toPlainString(),
                holdReference
        ));
    }

    public Money getAvailableBalance() {
        return this.balance.subtract(this.holdBalance);
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(this.domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    protected Account() {
        this.id = null;
        this.tenantId = null;
        this.customerId = null;
        this.currency = null;
    }
}
