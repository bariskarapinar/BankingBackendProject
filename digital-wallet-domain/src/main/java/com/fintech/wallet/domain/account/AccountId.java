package com.fintech.wallet.domain.account;

import com.fintech.wallet.common.exception.DomainException;
import lombok.Value;
import java.util.UUID;

@Value
public class AccountId {
    String value;

    public AccountId(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(
                    "AccountId cannot be null or empty",
                    "INVALID_ACCOUNT_ID",
                    ""
            );
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    "AccountId must be a valid UUID",
                    "INVALID_ACCOUNT_ID",
                    "Provided: " + value,
                    e
            );
        }
        this.value = value;
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID().toString());
    }
}
