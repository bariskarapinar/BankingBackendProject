package com.fintech.wallet.application.command;

import com.fintech.wallet.common.model.Command;
import lombok.Value;

@Value
public class CreateAccountCommand implements Command {
    String tenantId;
    String customerId;
    String currency;
    String initialBalance;
}
