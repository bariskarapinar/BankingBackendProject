package com.fintech.wallet.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferSummaryQueryPort {
    
    /**
     * Get transfer summary for reporting
     */
    Optional<TransferSummaryView> getTransferSummary(String transferId);

    /**
     * Get transfers by source account (for account history)
     */
    List<TransferSummaryView> getTransfersBySource(String sourceAccountId);

    /**
     * Get transfers by destination account
     */
    List<TransferSummaryView> getTransfersByDestination(String destinationAccountId);

    /**
     * Get transfers by status (for dashboard)
     */
    List<TransferSummaryView> getTransfersByStatus(String status);

    /**
     * Get transfers within date range
     */
    List<TransferSummaryView> getTransfersByDateRange(Instant startDate, Instant endDate);

    /**
     * Get total transfer volume (for business metrics)
     */
    BigDecimal getTotalTransferVolume(Instant startDate, Instant endDate);

    /**
     * Get transfer count by status
     */
    java.util.Map<String, Long> getTransferCountByStatus();

    public class TransferSummaryView {
        public final String transferId;
        public final String sourceAccountId;
        public final String destinationAccountId;
        public final BigDecimal amount;
        public final String status;
        public final String riskLevel;
        public final Instant createdAt;
        public final Instant completedAt;
        public final boolean blocked;

        public TransferSummaryView(String transferId, String sourceAccountId, String destinationAccountId,
                                  BigDecimal amount, String status, String riskLevel,
                                  Instant createdAt, Instant completedAt, boolean blocked) {
            this.transferId = transferId;
            this.sourceAccountId = sourceAccountId;
            this.destinationAccountId = destinationAccountId;
            this.amount = amount;
            this.status = status;
            this.riskLevel = riskLevel;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
            this.blocked = blocked;
        }
    }
}
