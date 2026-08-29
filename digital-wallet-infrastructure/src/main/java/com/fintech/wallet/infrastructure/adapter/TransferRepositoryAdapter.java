package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.port.TransferRepository;
import com.fintech.wallet.domain.transfer.Transfer;
import com.fintech.wallet.infrastructure.entity.TransferEntity;
import com.fintech.wallet.infrastructure.repository.TransferSpringDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransferRepositoryAdapter implements TransferRepository {
    private final TransferSpringDataRepository repository;

    @Override
    public Transfer save(Transfer transfer) {
        TransferEntity entity = TransferEntity.from(transfer);
        TransferEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Transfer> findById(String transferId) {
        return repository.findById(transferId)
                .map(TransferEntity::toDomain);
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(TransferEntity::toDomain);
    }
}
