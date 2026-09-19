package com.fintech.wallet.application.saga;

import com.fintech.wallet.application.command.InitiateP2PTransferCommand;
import com.fintech.wallet.application.dto.TransferResponseDTO;
import com.fintech.wallet.application.port.*;
import com.fintech.wallet.common.exception.ApplicationException;
import com.fintech.wallet.domain.account.Account;
import com.fintech.wallet.domain.account.AccountId;
import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.domain.fraud.FraudAssessment;
import com.fintech.wallet.domain.fraud.FraudRulesEngine;
import com.fintech.wallet.domain.transfer.Transfer;
import com.fintech.wallet.domain.transfer.TransferId;
import com.fintech.wallet.domain.transfer.TransferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class P2PTransferSaga {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventStorePort eventStorePort;
    private final ObjectMapper objectMapper;
    private final FraudScoringPort fraudScoringPort;
    private final DistributedLockPort distributedLockPort;
    private final FraudRulesEngine fraudRulesEngine;
    
    private static final long LOCK_TIMEOUT_SECONDS = 10;

    /**
     * P2P Transfer Saga with Choreography Pattern
     * 
     * Saga Flow:
     * 1. Check idempotency (prevent duplicate transfers)
     * 2. Validate accounts exist
     * 3. Fraud assessment & scoring
     * 4. Reserve debit on source account
     * 5. Credit destination account
     * 6. Complete transfer or compensate
     */
    @Transactional
    public TransferResponseDTO initiateP2PTransfer(InitiateP2PTransferCommand command) {
        try {
            log.info("Initiating P2P Transfer - Source: {}, Destination: {}, Amount: {}",
                    command.getSourceAccountId(), command.getDestinationAccountId(), command.getAmount());

            // Step 1: Check Idempotency (Prevent Duplicate Transfers)
            Optional<Transfer> existingTransfer = transferRepository.findByIdempotencyKey(command.getIdempotencyKey());
            if (existingTransfer.isPresent()) {
                log.info("Idempotent transfer detected - returning existing transfer: {}", 
                        existingTransfer.get().getId().getValue());
                return mapToResponseDTO(existingTransfer.get());
            }

            // Step 2: Create Transfer Aggregate
            TransferId transferId = TransferId.generate();
            AccountId sourceAccountId = new AccountId(command.getSourceAccountId());
            AccountId destAccountId = new AccountId(command.getDestinationAccountId());
            Currency currency = Currency.getInstance("USD");
            Money transferAmount = new Money(new BigDecimal(command.getAmount()), currency);

            Transfer transfer = new Transfer(
                    transferId,
                    command.getTenantId(),
                    sourceAccountId,
                    destAccountId,
                    transferAmount,
                    command.getIdempotencyKey()
            );

            persistDomainEvents(transfer);
            transfer = transferRepository.save(transfer);

            // Step 3: Load Both Accounts
            Account sourceAccount = accountRepository.findById(sourceAccountId)
                    .orElseThrow(() -> new ApplicationException(
                            "Source account not found",
                            "ACCOUNT_NOT_FOUND",
                            "AccountId: " + command.getSourceAccountId(),
                            null
                    ));

            Account destAccount = accountRepository.findById(destAccountId)
                    .orElseThrow(() -> new ApplicationException(
                            "Destination account not found",
                            "ACCOUNT_NOT_FOUND",
                            "AccountId: " + command.getDestinationAccountId(),
                            null
                    ));

            // Step 4: Mark Transfer as Validation Pending
            transfer.markAsValidationPending();

            // Step 5: Fraud Assessment & Scoring
            FraudAssessment fraudAssessment = performFraudCheck(transfer, sourceAccount);
            
            if (fraudAssessment.shouldBlock()) {
                log.warn("Transfer blocked by fraud check - Transfer: {}, Risk: {}",
                        transferId.getValue(), fraudAssessment.getRiskScore().getRiskLevel());
                transfer.markAsFraudRejected(fraudAssessment.getSummary());
                persistDomainEvents(transfer);
                transferRepository.save(transfer);

                throw new ApplicationException(
                        "Transfer rejected by fraud check",
                        "FRAUD_CHECK_FAILED",
                        fraudAssessment.getSummary(),
                        null
                );
            }

            // Step 6: Acquire Distributed Locks (Prevent Concurrent Modifications)
            String sourceLockKey = buildLockKey(command.getTenantId(), command.getSourceAccountId());
            String destLockKey = buildLockKey(command.getTenantId(), command.getDestinationAccountId());

            boolean sourceLockAcquired = distributedLockPort.tryLock(sourceLockKey, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!sourceLockAcquired) {
                throw new ApplicationException(
                        "Cannot acquire lock on source account",
                        "LOCK_ACQUISITION_FAILED",
                        "Source account is currently being modified",
                        null
                );
            }

            try {
                boolean destLockAcquired = distributedLockPort.tryLock(destLockKey, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!destLockAcquired) {
                    throw new ApplicationException(
                            "Cannot acquire lock on destination account",
                            "LOCK_ACQUISITION_FAILED",
                            "Destination account is currently being modified",
                            null
                    );
                }

                try {
                    return executeTransferWithLocks(command, transfer, sourceAccount, destAccount);
                } finally {
                    distributedLockPort.unlock(destLockKey);
                }
            } finally {
                distributedLockPort.unlock(sourceLockKey);
            }

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during P2P transfer", e);
            throw new ApplicationException(
                    "Unexpected error during transfer",
                    "TRANSFER_ERROR",
                    e.getMessage(),
                    e
            );
        }
    }

    private TransferResponseDTO executeTransferWithLocks(InitiateP2PTransferCommand command, 
                                                         Transfer transfer,
                                                         Account sourceAccount, 
                                                         Account destAccount) {
        // Reload accounts with pessimistic locks
        sourceAccount = accountRepository.findByIdWithLock(new AccountId(command.getSourceAccountId()))
                .orElseThrow(() -> new ApplicationException(
                        "Source account not found",
                        "ACCOUNT_NOT_FOUND",
                        "",
                        null
                ));

        destAccount = accountRepository.findByIdWithLock(new AccountId(command.getDestinationAccountId()))
                .orElseThrow(() -> new ApplicationException(
                        "Destination account not found",
                        "ACCOUNT_NOT_FOUND",
                        "",
                        null
                ));

        Money transferAmount = new Money(new BigDecimal(command.getAmount()), sourceAccount.getCurrency());

        try {
            // Step 7: Reserve Debit on Source (Hold Balance)
            sourceAccount.holdBalance(transferAmount, "transfer-" + transfer.getId().getValue());
            transfer.markAsDebitReserved();
            accountRepository.save(sourceAccount);
            transferRepository.save(transfer);

            log.debug("Debit reserved on source account - Amount: {}", command.getAmount());

            // Step 8: Process Credit on Destination
            transfer.markAsCreditProcessing();
            destAccount.depositFunds(transferAmount);
            accountRepository.save(destAccount);
            transferRepository.save(transfer);

            // Step 9: Release Hold & Mark Complete
            sourceAccount.releaseHold(transferAmount, "transfer-" + transfer.getId().getValue());
            sourceAccount.withdrawFunds(transferAmount);
            accountRepository.save(sourceAccount);

            transfer.markAsCompleted();
            persistDomainEvents(transfer);
            Transfer finalTransfer = transferRepository.save(transfer);

            log.info("P2P Transfer completed successfully - Transfer ID: {}", transfer.getId().getValue());
            return mapToResponseDTO(finalTransfer);

        } catch (Exception e) {
            log.error("Error during transfer execution - attempting compensation", e);
            // Compensation: Release hold on source account
            try {
                sourceAccount.releaseHold(transferAmount, "transfer-" + transfer.getId().getValue());
                accountRepository.save(sourceAccount);
                transfer.markAsCompensated();
            } catch (Exception compensationError) {
                log.error("Compensation failed", compensationError);
                transfer.markAsFailed("Transfer failed and compensation also failed: " + compensationError.getMessage());
            }

            transferRepository.save(transfer);
            persistDomainEvents(transfer);
            transfer.clearDomainEvents();

            throw new ApplicationException(
                    "Transfer processing failed: " + e.getMessage(),
                    "TRANSFER_PROCESSING_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    private FraudAssessment performFraudCheck(Transfer transfer, Account sourceAccount) {
        int hourOfDay = Instant.now().atZone(ZoneId.systemDefault()).getHour();
        
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                transfer.getId().getValue(),
                transfer.getAmount(),
                hourOfDay,
                true,  // Assume new destination (Phase 2 enhancement: check if actually new)
                false, // Assume not rapid succession (Phase 2 enhancement: check recent transfers)
                false  // Assume no geographic anomaly (Phase 2 enhancement: check location data)
        );

        return fraudScoringPort.scoreTransfer(context);
    }

    private void persistDomainEvents(Transfer transfer) {
        java.util.List<com.fintech.wallet.common.model.DomainEvent> events = transfer.getDomainEvents();
        long sequence = eventStorePort.getEventCount(transfer.getId().getValue());
        for (com.fintech.wallet.common.model.DomainEvent event : events) {
            com.fintech.wallet.application.event.OutboxEvent outboxEvent = 
                    com.fintech.wallet.application.event.OutboxEvent.from(event);
            outboxEventRepository.save(outboxEvent);
            eventStorePort.appendEvent(com.fintech.wallet.domain.event.sourcing.StoredEvent.from(
                    event,
                    transfer.getId().getValue(),
                    "Transfer",
                    ++sequence,
                    serializeEvent(event),
                    transfer.getTenantId()
            ));
        }
        transfer.clearDomainEvents();
    }

    private String serializeEvent(com.fintech.wallet.common.model.DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(
                    "Failed to serialize domain event",
                    "EVENT_SERIALIZATION_FAILED",
                    event.getEventType(),
                    e
            );
        }
    }

    private String buildLockKey(String tenantId, String accountId) {
        return "account-lock:" + tenantId + ":" + accountId;
    }

    private TransferResponseDTO mapToResponseDTO(Transfer transfer) {
        return TransferResponseDTO.builder()
                .transferId(transfer.getId().getValue())
                .status(transfer.getStatus().name())
                .sourceAccountId(transfer.getSourceAccountId().getValue())
                .destinationAccountId(transfer.getDestinationAccountId().getValue())
                .amount(transfer.getAmount().getAmount().toPlainString())
                .failureReason(transfer.getFailureReason())
                .version(transfer.getVersion())
                .build();
    }
}
