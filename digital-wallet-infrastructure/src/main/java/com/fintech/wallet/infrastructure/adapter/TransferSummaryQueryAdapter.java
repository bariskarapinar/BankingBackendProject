package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.port.TransferSummaryQueryPort;
import com.fintech.wallet.infrastructure.entity.TransferEntity;
import com.fintech.wallet.infrastructure.repository.TransferSpringDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransferSummaryQueryAdapter implements TransferSummaryQueryPort {
    private final TransferSpringDataRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferSummaryView> getTransferSummary(String transferId) {
        return repository.findById(transferId).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummaryView> getTransfersBySource(String sourceAccountId) {
        return repository.findBySourceAccountIdOrderByCreatedAtDesc(sourceAccountId)
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummaryView> getTransfersByDestination(String destinationAccountId) {
        return repository.findByDestinationAccountIdOrderByCreatedAtDesc(destinationAccountId)
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummaryView> getTransfersByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtDesc(
                        com.fintech.wallet.domain.transfer.TransferStatus.valueOf(status))
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummaryView> getTransfersByDateRange(Instant startDate, Instant endDate) {
        return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate)
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalTransferVolume(Instant startDate, Instant endDate) {
        return repository.sumAmountByCreatedAtBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getTransferCountByStatus() {
        return repository.countByStatus().stream().collect(Collectors.toMap(
                row -> ((com.fintech.wallet.domain.transfer.TransferStatus) row[0]).name(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    private TransferSummaryView toView(TransferEntity entity) {
        String status = entity.getStatus().name();
        boolean completed = entity.getStatus() == com.fintech.wallet.domain.transfer.TransferStatus.COMPLETED;
        return new TransferSummaryView(
                entity.getTransferId(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                entity.getAmount(),
                status,
                null,
                entity.getCreatedAt(),
                completed ? entity.getUpdatedAt() : null,
                entity.getStatus() == com.fintech.wallet.domain.transfer.TransferStatus.FRAUD_REJECTED
        );
    }
}
