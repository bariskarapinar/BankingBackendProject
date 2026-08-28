package com.fintech.wallet.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryJpaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LedgerEntryType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String reference;

    @Column
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum LedgerEntryType {
        DEBIT, CREDIT, HOLD, RELEASE
    }
}
