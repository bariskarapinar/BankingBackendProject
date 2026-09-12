package com.fintech.wallet.domain.account;

import com.fintech.wallet.common.exception.DomainException;
import lombok.Value;
import java.math.BigDecimal;
import java.util.Currency;

@Value
public class Money {
    BigDecimal amount;
    Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.signum() < 0) {
            throw new DomainException(
                    "Money amount cannot be null or negative",
                    "INVALID_MONEY",
                    "Provided amount: " + amount
            );
        }
        if (currency == null) {
            throw new DomainException(
                    "Currency cannot be null",
                    "INVALID_CURRENCY",
                    ""
            );
        }
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), java.math.RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(
                    "Cannot add money with different currencies",
                    "CURRENCY_MISMATCH",
                    this.currency + " vs " + other.currency
            );
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(
                    "Cannot subtract money with different currencies",
                    "CURRENCY_MISMATCH",
                    this.currency + " vs " + other.currency
            );
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(
                    "Cannot compare money with different currencies",
                    "CURRENCY_MISMATCH",
                    this.currency + " vs " + other.currency
            );
        }
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(
                    "Cannot compare money with different currencies",
                    "CURRENCY_MISMATCH",
                    this.currency + " vs " + other.currency
            );
        }
        return this.amount.compareTo(other.amount) <= 0;
    }
}
