package com.fintech.wallet.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.wallet.application.dto.TransferResponseDTO;
import com.fintech.wallet.application.port.EventStorePort;
import com.fintech.wallet.common.exception.ApplicationException;
import com.fintech.wallet.domain.account.AccountId;
import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.domain.event.sourcing.StoredEvent;
import com.fintech.wallet.domain.transfer.Transfer;
import com.fintech.wallet.domain.transfer.TransferId;
import com.fintech.wallet.domain.transfer.TransferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferEventReplayService {
    private final EventStorePort eventStorePort;
    private final ObjectMapper objectMapper;

    public TransferResponseDTO replay(String transferId, String tenantId) {
        List<StoredEvent> events = eventStorePort.getEventsForAggregate(transferId);
        if (events.isEmpty()) {
            throw new ApplicationException(
                    "Transfer event stream not found",
                    "TRANSFER_EVENTS_NOT_FOUND",
                    transferId,
                    null
            );
        }

        Transfer transfer = createFromInitiatedEvent(events.get(0), transferId);
        for (StoredEvent event : events.subList(1, events.size())) {
            applyStateChange(transfer, event);
        }

        if (!tenantId.equals(transfer.getTenantId())) {
            throw new ApplicationException(
                    "Transfer event stream not found",
                    "TRANSFER_EVENTS_NOT_FOUND",
                    transferId,
                    null
            );
        }

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

    private Transfer createFromInitiatedEvent(StoredEvent storedEvent, String transferId) {
        try {
            JsonNode payload = objectMapper.readTree(storedEvent.getEventPayload());
            Transfer transfer = new Transfer(
                    new TransferId(transferId),
                    requiredText(payload, "tenantId", storedEvent.getTenantId()),
                    new AccountId(requiredText(payload, "sourceAccountId", null)),
                    new AccountId(requiredText(payload, "destinationAccountId", null)),
                    new Money(new BigDecimal(requiredText(payload, "amount", null)),
                            Currency.getInstance("USD")),
                    requiredText(payload, "idempotencyKey", null)
            );
            transfer.clearDomainEvents();
            return transfer;
        } catch (Exception e) {
            throw replayFailure(transferId, e);
        }
    }

    private void applyStateChange(Transfer transfer, StoredEvent event) {
        if (!"TransferStateChangedEvent".equals(event.getEventType())) {
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(event.getEventPayload());
            String status = requiredText(payload, "currentStatus", null);
            String failureReason = textOrNull(payload, "failureReason");
            transfer.restoreState(
                    TransferStatus.valueOf(status),
                    failureReason,
                    event.getTimestamp(),
                    event.getSequenceNumber()
            );
        } catch (Exception e) {
            throw replayFailure(transfer.getId().getValue(), e);
        }
    }

    private String requiredText(JsonNode payload, String field, String fallback) {
        JsonNode value = payload.get(field);
        if (value != null && !value.isNull() && !value.asText().isBlank()) {
            return value.asText();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        throw new IllegalArgumentException("Missing event field: " + field);
    }

    private String textOrNull(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private ApplicationException replayFailure(String transferId, Exception cause) {
        return new ApplicationException(
                "Failed to replay transfer event stream",
                "TRANSFER_REPLAY_FAILED",
                transferId,
                cause
        );
    }
}
