package com.fintech.wallet.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountResponseDTO {
    String accountId;
    String tenantId;
    String customerId;
    String currency;
    String balance;
    String holdBalance;
    String availableBalance;
    long version;
}
