package com.fintech.wallet.domain.event;

import com.fintech.wallet.common.model.DomainEvent;
import lombok.Getter;

@Getter
public class AccountCreatedEvent extends DomainEvent {
    private final String tenantId;
    private final String customerId;
    private final String currency;
    private final String initialBalance;

    public AccountCreatedEvent(String accountId, String tenantId, String customerId, String currency, String initialBalance) {
        super(accountId, "Account");
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.currency = currency;
        this.initialBalance = initialBalance;
    }
}
