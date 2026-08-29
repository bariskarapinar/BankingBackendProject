package com.fintech.wallet.domain.transfer;

import com.fintech.wallet.common.exception.DomainException;
import lombok.Value;
import java.util.UUID;

@Value
public class TransferId {
    String value;

    public TransferId(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(
                    "TransferId cannot be null or empty",
                    "INVALID_TRANSFER_ID",
                    ""
            );
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    "TransferId must be a valid UUID",
                    "INVALID_TRANSFER_ID",
                    "Provided: " + value,
                    e
            );
        }
        this.value = value;
    }

    public static TransferId generate() {
        return new TransferId(UUID.randomUUID().toString());
    }
}
