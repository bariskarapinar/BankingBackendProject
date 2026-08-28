package com.fintech.wallet.domain.ledger;

import com.fintech.wallet.common.exception.DomainException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Getter
public class LedgerEntry {
    private final String id;
    private final String accountId;
    private final String tenantId;
    private final LedgerEntryType type;
    private final BigDecimal amount;
    private final Currency currency;
    private final String reference;
    private final String description;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;

    public enum LedgerEntryType {
        DEBIT, CREDIT, HOLD, RELEASE
    }

    public LedgerEntry(String id, String accountId, String tenantId, LedgerEntryType type,
                       BigDecimal amount, Currency currency, String reference, String description) {
        if (id == null || accountId == null || tenantId == null || type == null ||
                amount == null || currency == null || reference == null) {
            throw new DomainException(
                    "LedgerEntry fields cannot be null",
                    "INVALID_LEDGER_ENTRY",
                    ""
            );
        }
        if (amount.signum() <= 0) {
            throw new DomainException(
                    "LedgerEntry amount must be positive",
                    "INVALID_LEDGER_AMOUNT",
                    "Provided: " + amount
            );
        }
        this.id = id;
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.type = type;
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), java.math.RoundingMode.HALF_UP);
        this.currency = currency;
        this.reference = reference;
        this.description = description;
        this.createdAt = Instant.now();
    }

    protected LedgerEntry() {
        this.id = null;
        this.accountId = null;
        this.tenantId = null;
        this.type = null;
        this.amount = null;
        this.currency = null;
        this.reference = null;
        this.description = null;
    }
}
