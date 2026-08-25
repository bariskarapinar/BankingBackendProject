package com.fintech.wallet.domain.event;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;

@Getter
public class TransferInitiatedEvent extends DomainEvent {
    private final String tenantId;
    private final String sourceAccountId;
    private final String destinationAccountId;
    private final String amount;
    private final String idempotencyKey;

    public TransferInitiatedEvent(String transferId, String tenantId, String sourceAccountId,
                                   String destinationAccountId, String amount, String idempotencyKey) {
        super(transferId, "Transfer");
        this.tenantId = tenantId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
