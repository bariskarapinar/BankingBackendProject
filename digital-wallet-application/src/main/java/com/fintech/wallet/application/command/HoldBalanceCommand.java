package com.fintech.wallet.application.command;

import com.fintech.wallet.common.model.Command;
import lombok.Value;

@Value
public class HoldBalanceCommand implements Command {
    String accountId;
    String amount;
    String holdReference;
    String tenantId;
}
