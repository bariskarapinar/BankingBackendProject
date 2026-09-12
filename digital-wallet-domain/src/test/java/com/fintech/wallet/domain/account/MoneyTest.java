package com.fintech.wallet.domain.account;

import com.fintech.wallet.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    private final Currency USD = Currency.getInstance("USD");
    private final Currency EUR = Currency.getInstance("EUR");

    @Test
    @DisplayName("Should create Money with valid amount and currency")
    void testCreateMoneySuccess() {
        Money money = new Money(BigDecimal.valueOf(100.50), USD);

        assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.50));
        assertThat(money.getCurrency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("Should reject negative amount")
    void testCreateMoneyWithNegativeAmount() {
        assertThatThrownBy(() -> new Money(BigDecimal.valueOf(-10), USD))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_MONEY");
    }

    @Test
    @DisplayName("Should reject null currency")
    void testCreateMoneyWithNullCurrency() {
        assertThatThrownBy(() -> new Money(BigDecimal.valueOf(100), null))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_CURRENCY");
    }

    @Test
    @DisplayName("Should add money successfully with same currency")
    void testAddMoneySuccess() {
        Money money1 = new Money(BigDecimal.valueOf(100), USD);
        Money money2 = new Money(BigDecimal.valueOf(50), USD);

        Money result = money1.add(money2);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(result.getCurrency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("Should reject adding money with different currencies")
    void testAddMoneyWithDifferentCurrency() {
        Money money1 = new Money(BigDecimal.valueOf(100), USD);
        Money money2 = new Money(BigDecimal.valueOf(50), EUR);

        assertThatThrownBy(() -> money1.add(money2))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo("CURRENCY_MISMATCH");
    }

    @Test
    @DisplayName("Should subtract money successfully")
    void testSubtractMoneySuccess() {
        Money money1 = new Money(BigDecimal.valueOf(100), USD);
        Money money2 = new Money(BigDecimal.valueOf(30), USD);

        Money result = money1.subtract(money2);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    @Test
    @DisplayName("Should compare money amounts correctly")
    void testMoneyComparison() {
        Money money1 = new Money(BigDecimal.valueOf(100), USD);
        Money money2 = new Money(BigDecimal.valueOf(50), USD);

        assertThat(money1.isGreaterThanOrEqual(money2)).isTrue();
        assertThat(money2.isLessThanOrEqual(money1)).isTrue();
    }
}
