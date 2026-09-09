package com.fintech.wallet.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequestDTO {
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Initial balance is required")
    @Positive(message = "Initial balance must be positive")
    private String initialBalance;
}
