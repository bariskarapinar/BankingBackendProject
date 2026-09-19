package com.fintech.wallet.application.port;

import com.fintech.wallet.domain.transfer.Transfer;
import java.util.Optional;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(String transferId);
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
