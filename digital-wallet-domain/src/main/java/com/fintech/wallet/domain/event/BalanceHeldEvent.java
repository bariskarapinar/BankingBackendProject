package com.fintech.wallet.domain.event;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;

@Getter
public class BalanceHeldEvent extends DomainEvent {
    private final String amount;
    private final String holdReference;

    public BalanceHeldEvent(String accountId, String amount, String holdReference) {
        super(accountId, "Account");
        this.amount = amount;
        this.holdReference = holdReference;
    }
}
