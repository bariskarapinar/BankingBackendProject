package com.fintech.wallet.domain.transfer;

import com.fintech.wallet.common.exception.DomainException;
import com.fintech.wallet.common.model.DomainEvent;
import com.fintech.wallet.domain.account.AccountId;
import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.domain.event.TransferInitiatedEvent;
import com.fintech.wallet.domain.event.TransferCompletedEvent;
import com.fintech.wallet.domain.event.TransferFailedEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Getter
public class Transfer {
    private final TransferId id;
    private final String tenantId;
    private final AccountId sourceAccountId;
    private final AccountId destinationAccountId;
    private final Money amount;
    private final String idempotencyKey;
    
    @Setter(AccessLevel.PACKAGE)
    private TransferStatus status;
    
    @Setter(AccessLevel.PACKAGE)
    private String failureReason;
    
    @Setter(AccessLevel.PACKAGE)
    private long version;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;
    
    @Setter(AccessLevel.PACKAGE)
    private Instant updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Transfer(TransferId id, String tenantId, AccountId sourceAccountId, 
                    AccountId destinationAccountId, Money amount, String idempotencyKey) {
        if (id == null || tenantId == null || sourceAccountId == null || 
                destinationAccountId == null || amount == null || idempotencyKey == null) {
            throw new DomainException(
                    "Transfer fields cannot be null",
                    "INVALID_TRANSFER_CREATION",
                    ""
            );
        }
        
        if (sourceAccountId.getValue().equals(destinationAccountId.getValue())) {
            throw new DomainException(
                    "Cannot transfer to the same account",
                    "INVALID_TRANSFER",
                    "Source and destination cannot be identical"
            );
        }

        this.id = id;
        this.tenantId = tenantId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = TransferStatus.INITIATED;
        this.version = 1;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        this.domainEvents.add(new TransferInitiatedEvent(
                id.getValue(),
                tenantId,
                sourceAccountId.getValue(),
                destinationAccountId.getValue(),
                amount.getAmount().toPlainString(),
                idempotencyKey
        ));
    }

    public void markAsValidationPending() {
        if (this.status != TransferStatus.INITIATED) {
            throw new DomainException(
                    "Transfer is not in INITIATED status",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.PENDING_VALIDATION;
        this.updatedAt = Instant.now();
    }

    public void markAsDebitReserved() {
        if (this.status != TransferStatus.PENDING_VALIDATION) {
            throw new DomainException(
                    "Transfer is not in PENDING_VALIDATION status",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.DEBIT_RESERVED;
        this.updatedAt = Instant.now();
    }

    public void markAsCreditProcessing() {
        if (this.status != TransferStatus.DEBIT_RESERVED) {
            throw new DomainException(
                    "Transfer is not in DEBIT_RESERVED status",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.CREDIT_PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markAsCompleted() {
        if (this.status != TransferStatus.CREDIT_PROCESSING) {
            throw new DomainException(
                    "Transfer is not in CREDIT_PROCESSING status",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.COMPLETED;
        this.updatedAt = Instant.now();

        this.domainEvents.add(new TransferCompletedEvent(
                this.id.getValue(),
                this.amount.getAmount().toPlainString()
        ));
    }

    public void markAsFailed(String reason) {
        if (this.status.isTerminal()) {
            throw new DomainException(
                    "Cannot fail a transfer in terminal status",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();

        this.domainEvents.add(new TransferFailedEvent(
                this.id.getValue(),
                reason
        ));
    }

    public void markAsFraudRejected(String reason) {
        if (this.status != TransferStatus.PENDING_VALIDATION) {
            throw new DomainException(
                    "Transfer must be in PENDING_VALIDATION to reject as fraud",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.FRAUD_REJECTED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();

        this.domainEvents.add(new TransferFailedEvent(
                this.id.getValue(),
                "Fraud rejected: " + reason
        ));
    }

    public void markAsCompensated() {
        if (!this.status.canBeRolledBack()) {
            throw new DomainException(
                    "Transfer cannot be compensated from current status",
                    "INVALID_TRANSFER_STATE",
                    "Current status: " + this.status
            );
        }
        this.status = TransferStatus.COMPENSATED;
        this.failureReason = "Transfer compensated";
        this.updatedAt = Instant.now();
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(this.domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    protected Transfer() {
        this.id = null;
        this.tenantId = null;
        this.sourceAccountId = null;
        this.destinationAccountId = null;
        this.amount = null;
        this.idempotencyKey = null;
    }
}
