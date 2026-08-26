package com.fintech.wallet.domain.event;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;

@Getter
public class TransferCompletedEvent extends DomainEvent {
    private final String amount;

    public TransferCompletedEvent(String transferId, String amount) {
        super(transferId, "Transfer");
        this.amount = amount;
    }
}
