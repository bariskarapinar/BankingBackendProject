package com.fintech.wallet.domain.event;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;

@Getter
public class TransferStateChangedEvent extends DomainEvent {
    private final String tenantId;
    private final String previousStatus;
    private final String currentStatus;
    private final String failureReason;

    public TransferStateChangedEvent(String transferId, String tenantId,
                                     String previousStatus, String currentStatus,
                                     String failureReason) {
        super(transferId, "Transfer");
        this.tenantId = tenantId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.failureReason = failureReason;
    }
}
