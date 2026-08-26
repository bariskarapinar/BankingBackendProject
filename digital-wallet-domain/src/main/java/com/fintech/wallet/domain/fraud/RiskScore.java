package com.fintech.wallet.domain.fraud;

import com.fintech.wallet.common.exception.DomainException;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class RiskScore {
    private final BigDecimal score;  // 0.0 to 1.0
    private final RiskLevel riskLevel;
    private final String reason;

    public enum RiskLevel {
        LOW,           // score: 0.0 - 0.3
        MEDIUM,        // score: 0.3 - 0.7
        HIGH,          // score: 0.7 - 0.9
        CRITICAL       // score: 0.9 - 1.0
    }

    public RiskScore(BigDecimal score, String reason) {
        if (score == null) {
            throw new DomainException(
                    "Risk score cannot be null",
                    "INVALID_RISK_SCORE",
                    ""
            );
        }

        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new DomainException(
                    "Risk score must be between 0.0 and 1.0",
                    "INVALID_RISK_SCORE",
                    "Provided: " + score
            );
        }

        this.score = score;
        this.reason = reason;
        this.riskLevel = determineRiskLevel(score);
    }

    private RiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(0.9)) >= 0) {
            return RiskLevel.CRITICAL;
        } else if (score.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return RiskLevel.HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(0.3)) >= 0) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    public boolean shouldBlock() {
        return this.riskLevel == RiskLevel.CRITICAL;
    }

    public boolean requiresReview() {
        return this.riskLevel == RiskLevel.HIGH || this.riskLevel == RiskLevel.CRITICAL;
    }
}
