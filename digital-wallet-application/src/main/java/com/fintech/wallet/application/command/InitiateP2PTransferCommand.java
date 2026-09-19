package com.fintech.wallet.application.command;

import com.fintech.wallet.common.model.Command;
import lombok.Value;

@Value
public class InitiateP2PTransferCommand implements Command {
    String sourceAccountId;
    String destinationAccountId;
    String amount;
    String tenantId;
    String idempotencyKey;  // For idempotent processing
}
