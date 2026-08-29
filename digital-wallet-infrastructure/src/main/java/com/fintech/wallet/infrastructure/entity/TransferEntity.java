package com.fintech.wallet.infrastructure.entity;

import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.domain.transfer.Transfer;
import com.fintech.wallet.domain.transfer.TransferId;
import com.fintech.wallet.domain.transfer.TransferStatus;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_source", columnList = "source_account_id"),
        @Index(name = "idx_transfer_dest", columnList = "destination_account_id"),
        @Index(name = "idx_transfer_status", columnList = "status"),
        @Index(name = "idx_transfer_idempotency", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TransferEntity {
    @Id
    @Column(name = "transfer_id", length = 36)
    private String transferId;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "source_account_id", length = 36, nullable = false)
    private String sourceAccountId;

    @Column(name = "destination_account_id", length = 36, nullable = false)
    private String destinationAccountId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;

    @Column(name = "idempotency_key", length = 256, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static TransferEntity from(Transfer transfer) {
        TransferEntity entity = new TransferEntity();
        entity.setTransferId(transfer.getId().getValue());
        entity.setTenantId(transfer.getTenantId());
        entity.setSourceAccountId(transfer.getSourceAccountId().getValue());
        entity.setDestinationAccountId(transfer.getDestinationAccountId().getValue());
        entity.setAmount(transfer.getAmount().getAmount());
        entity.setCurrency(transfer.getAmount().getCurrency().getCurrencyCode());
        entity.setStatus(transfer.getStatus());
        entity.setIdempotencyKey(transfer.getIdempotencyKey());
        entity.setFailureReason(transfer.getFailureReason());
        entity.setCreatedAt(transfer.getCreatedAt());
        entity.setUpdatedAt(transfer.getUpdatedAt());
        entity.setVersion(transfer.getVersion());
        return entity;
    }

    public Transfer toDomain() {
        return new Transfer(
                new TransferId(this.transferId),
                this.tenantId,
                new com.fintech.wallet.domain.account.AccountId(this.sourceAccountId),
                new com.fintech.wallet.domain.account.AccountId(this.destinationAccountId),
                new Money(this.amount, Currency.getInstance(this.currency)),
                this.idempotencyKey,
                this.status,
                this.failureReason,
                this.createdAt,
                this.updatedAt,
                this.version
        );
    }
}
