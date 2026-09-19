package com.fintech.wallet.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransferResponseDTO {
    String transferId;
    String status;
    String sourceAccountId;
    String destinationAccountId;
    String amount;
    String failureReason;
    long version;
}
