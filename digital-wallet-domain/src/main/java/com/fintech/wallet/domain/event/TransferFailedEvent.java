package com.fintech.wallet.domain.event;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;

@Getter
public class TransferFailedEvent extends DomainEvent {
    private final String reason;

    public TransferFailedEvent(String transferId, String reason) {
        super(transferId, "Transfer");
        this.reason = reason;
    }
}
