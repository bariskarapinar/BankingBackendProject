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
public class DepositFundsRequestDTO {
    @NotBlank(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private String amount;
}
