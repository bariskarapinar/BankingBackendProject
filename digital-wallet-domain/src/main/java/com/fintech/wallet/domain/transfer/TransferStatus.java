package com.fintech.wallet.domain.transfer;

import java.time.Instant;

public enum TransferStatus {
    INITIATED("Transfer initiated"),
    PENDING_VALIDATION("Awaiting validation"),
    FRAUD_REJECTED("Rejected by fraud check"),
    DEBIT_RESERVED("Debit amount reserved on source"),
    DEBIT_FAILED("Failed to reserve debit"),
    CREDIT_PROCESSING("Credit in progress"),
    COMPLETED("Transfer completed successfully"),
    FAILED("Transfer failed"),
    COMPENSATED("Transfer compensated");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == COMPENSATED || this == FRAUD_REJECTED;
    }

    public boolean canBeRolledBack() {
        return this == DEBIT_RESERVED || this == DEBIT_FAILED || this == CREDIT_PROCESSING;
    }
}
