package com.fintech.wallet.domain.fraud;

import com.fintech.wallet.common.exception.DomainException;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
public class FraudAssessment {
    private final String transferId;
    private final List<String> triggeredRules;
    private final RiskScore riskScore;
    private final boolean shouldBlock;
    private final boolean requiresManualReview;

    public FraudAssessment(String transferId, List<String> triggeredRules, RiskScore riskScore) {
        if (transferId == null || triggeredRules == null || riskScore == null) {
            throw new DomainException(
                    "FraudAssessment fields cannot be null",
                    "INVALID_FRAUD_ASSESSMENT",
                    ""
            );
        }

        this.transferId = transferId;
        this.triggeredRules = new ArrayList<>(triggeredRules);
        this.riskScore = riskScore;
        this.shouldBlock = riskScore.shouldBlock();
        this.requiresManualReview = riskScore.requiresReview();
    }

    public String getSummary() {
        return String.format(
                "Risk Assessment: %s (Score: %.2f) - Triggered Rules: %s",
                riskScore.getRiskLevel(),
                riskScore.getScore(),
                triggeredRules.isEmpty() ? "None" : String.join(", ", triggeredRules)
        );
    }
}
