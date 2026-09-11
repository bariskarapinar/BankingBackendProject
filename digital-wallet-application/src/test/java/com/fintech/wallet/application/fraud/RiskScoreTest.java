package com.fintech.wallet.application.fraud;

import com.fintech.wallet.domain.fraud.RiskScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoreTest {

    @Test
    void shouldCalculateLowRiskLevel() {
        RiskScore riskScore = new RiskScore(BigDecimal.valueOf(0.25), "Low risk");
        assertEquals("LOW", riskScore.getRiskLevel());
        assertFalse(riskScore.shouldBlock());
        assertFalse(riskScore.requiresReview());
    }

    @Test
    void shouldCalculateMediumRiskLevel() {
        RiskScore riskScore = new RiskScore(BigDecimal.valueOf(0.5), "Medium risk");
        assertEquals("MEDIUM", riskScore.getRiskLevel());
        assertFalse(riskScore.shouldBlock());
        assertTrue(riskScore.requiresReview());
    }

    @Test
    void shouldCalculateHighRiskLevel() {
        RiskScore riskScore = new RiskScore(BigDecimal.valueOf(0.8), "High risk");
        assertEquals("HIGH", riskScore.getRiskLevel());
        assertTrue(riskScore.shouldBlock());
        assertTrue(riskScore.requiresReview());
    }

    @Test
    void shouldCalculateCriticalRiskLevel() {
        RiskScore riskScore = new RiskScore(BigDecimal.valueOf(0.95), "Critical risk");
        assertEquals("CRITICAL", riskScore.getRiskLevel());
        assertTrue(riskScore.shouldBlock());
        assertTrue(riskScore.requiresReview());
    }

    @Test
    void shouldHandleBoundaryScores() {
        // Lower boundary of MEDIUM (0.3)
        RiskScore medium = new RiskScore(BigDecimal.valueOf(0.3), "Boundary");
        assertEquals("MEDIUM", medium.getRiskLevel());

        // Lower boundary of HIGH (0.7)
        RiskScore high = new RiskScore(BigDecimal.valueOf(0.7), "Boundary");
        assertEquals("HIGH", high.getRiskLevel());

        // Lower boundary of CRITICAL (0.9)
        RiskScore critical = new RiskScore(BigDecimal.valueOf(0.9), "Boundary");
        assertEquals("CRITICAL", critical.getRiskLevel());
    }

    @Test
    void shouldThrowOnNullScore() {
        assertThrows(NullPointerException.class, () -> {
            new RiskScore(null, "description");
        });
    }

    @Test
    void shouldThrowOnNullDescription() {
        assertThrows(NullPointerException.class, () -> {
            new RiskScore(BigDecimal.valueOf(0.5), null);
        });
    }
}
