package com.fintech.wallet.api.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class InitiateTransferRequest {
    @NotBlank(message = "Source account ID is required")
    private String sourceAccountId;

    @NotBlank(message = "Destination account ID is required")
    private String destinationAccountId;

    @Positive(message = "Transfer amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required for transfer idempotency")
    private String idempotencyKey;
}
