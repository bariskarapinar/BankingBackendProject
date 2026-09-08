package com.fintech.wallet.domain.fraud;

import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.common.exception.DomainException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FraudRulesEngine {
    private static final BigDecimal HIGH_TRANSFER_THRESHOLD = BigDecimal.valueOf(50000);
    private static final BigDecimal SUSPICIOUS_TRANSFER_THRESHOLD = BigDecimal.valueOf(10000);
    private static final BigDecimal MICRO_TRANSFER_THRESHOLD = BigDecimal.valueOf(1);

    public FraudAssessment evaluate(FraudEvaluationContext context) {
        List<String> triggeredRules = new ArrayList<>();
        BigDecimal riskScore = BigDecimal.ZERO;

        // Rule 1: High Transfer Amount
        if (isHighTransferAmount(context.getTransferAmount())) {
            triggeredRules.add("HIGH_TRANSFER_AMOUNT");
            riskScore = riskScore.add(BigDecimal.valueOf(0.3));
        }

        // Rule 2: Suspicious Time
        if (isSuspiciousTime(context.getTransferHourOfDay())) {
            triggeredRules.add("SUSPICIOUS_TIME");
            riskScore = riskScore.add(BigDecimal.valueOf(0.15));
        }

        // Rule 3: New Destination Account
        if (context.isNewDestinationAccount()) {
            triggeredRules.add("NEW_DESTINATION_ACCOUNT");
            riskScore = riskScore.add(BigDecimal.valueOf(0.2));
        }

        // Rule 4: Rapid Succession Transfers
        if (context.isRapidSuccessionTransfer()) {
            triggeredRules.add("RAPID_SUCCESSION_TRANSFERS");
            riskScore = riskScore.add(BigDecimal.valueOf(0.25));
        }

        // Rule 5: Geographic Anomaly
        if (context.hasGeographicAnomaly()) {
            triggeredRules.add("GEOGRAPHIC_ANOMALY");
            riskScore = riskScore.add(BigDecimal.valueOf(0.35));
        }

        // Rule 6: Micro Transaction Pattern (possible probing)
        if (isMicroTransfer(context.getTransferAmount())) {
            triggeredRules.add("MICRO_TRANSFER_PATTERN");
            riskScore = riskScore.add(BigDecimal.valueOf(0.1));
        }

        // Cap risk score at 1.0
        if (riskScore.compareTo(BigDecimal.ONE) > 0) {
            riskScore = BigDecimal.ONE;
        }

        RiskScore finalRiskScore = new RiskScore(
                riskScore,
                String.format("Risk calculated from %d rules", triggeredRules.size())
        );

        return new FraudAssessment(context.getTransferId(), triggeredRules, finalRiskScore);
    }

    private boolean isHighTransferAmount(Money amount) {
        return amount.getAmount().compareTo(HIGH_TRANSFER_THRESHOLD) >= 0;
    }

    private boolean isSuspiciousTime(int hourOfDay) {
        // Between 2 AM and 5 AM
        return hourOfDay >= 2 && hourOfDay <= 5;
    }

    private boolean isMicroTransfer(Money amount) {
        return amount.getAmount().compareTo(MICRO_TRANSFER_THRESHOLD) < 0;
    }

    public static class FraudEvaluationContext {
        private final String transferId;
        private final Money transferAmount;
        private final int transferHourOfDay;
        private final boolean newDestinationAccount;
        private final boolean rapidSuccessionTransfer;
        private final boolean geographicAnomaly;

        public FraudEvaluationContext(String transferId, Money transferAmount, int transferHourOfDay,
                                      boolean newDestinationAccount, boolean rapidSuccessionTransfer,
                                      boolean geographicAnomaly) {
            if (transferId == null || transferAmount == null) {
                throw new DomainException(
                        "Transfer ID and amount cannot be null",
                        "INVALID_CONTEXT",
                        ""
                );
            }

            if (transferHourOfDay < 0 || transferHourOfDay > 23) {
                throw new DomainException(
                        "Hour of day must be between 0 and 23",
                        "INVALID_HOUR",
                        "Provided: " + transferHourOfDay
                );
            }

            this.transferId = transferId;
            this.transferAmount = transferAmount;
            this.transferHourOfDay = transferHourOfDay;
            this.newDestinationAccount = newDestinationAccount;
            this.rapidSuccessionTransfer = rapidSuccessionTransfer;
            this.geographicAnomaly = geographicAnomaly;
        }

        public String getTransferId() { return transferId; }
        public Money getTransferAmount() { return transferAmount; }
        public int getTransferHourOfDay() { return transferHourOfDay; }
        public boolean isNewDestinationAccount() { return newDestinationAccount; }
        public boolean isRapidSuccessionTransfer() { return rapidSuccessionTransfer; }
        public boolean hasGeographicAnomaly() { return geographicAnomaly; }
    }
}
