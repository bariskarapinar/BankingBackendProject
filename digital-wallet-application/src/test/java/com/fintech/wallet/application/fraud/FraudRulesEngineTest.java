package com.fintech.wallet.application.fraud;

import com.fintech.wallet.domain.account.Money;
import com.fintech.wallet.domain.fraud.FraudAssessment;
import com.fintech.wallet.domain.fraud.FraudRulesEngine;
import com.fintech.wallet.domain.fraud.RiskScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class FraudRulesEngineTest {
    private final FraudRulesEngine engine = new FraudRulesEngine();

    @Test
    void shouldDetectHighTransferAmountRule() {
        Money amount = new Money(BigDecimal.valueOf(60000), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-123",
                amount,
                10,
                false,
                false,
                false
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().contains("HIGH_TRANSFER_AMOUNT"));
        assertTrue(assessment.getRiskScore().getScore().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldDetectSuspiciousTimeRule() {
        Money amount = new Money(BigDecimal.valueOf(100), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-124",
                amount,
                3,  // 3 AM (suspicious)
                false,
                false,
                false
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().contains("SUSPICIOUS_TIME"));
    }

    @Test
    void shouldDetectNewDestinationAccountRule() {
        Money amount = new Money(BigDecimal.valueOf(5000), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-125",
                amount,
                10,
                true,  // New destination
                false,
                false
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().contains("NEW_DESTINATION_ACCOUNT"));
    }

    @Test
    void shouldDetectRapidSuccessionTransferRule() {
        Money amount = new Money(BigDecimal.valueOf(1000), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-126",
                amount,
                10,
                false,
                true,  // Rapid succession
                false
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().contains("RAPID_SUCCESSION_TRANSFERS"));
    }

    @Test
    void shouldDetectGeographicAnomalyRule() {
        Money amount = new Money(BigDecimal.valueOf(2000), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-127",
                amount,
                10,
                false,
                false,
                true  // Geographic anomaly
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().contains("GEOGRAPHIC_ANOMALY"));
    }

    @Test
    void shouldDetectMicroTransferPattern() {
        Money amount = new Money(BigDecimal.valueOf(0.50), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-128",
                amount,
                10,
                false,
                false,
                false
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().contains("MICRO_TRANSFER_PATTERN"));
    }

    @Test
    void shouldCombineMultipleRuleViolations() {
        Money amount = new Money(BigDecimal.valueOf(75000), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-129",
                amount,
                4,  // 4 AM (suspicious)
                true,  // New destination
                true,  // Rapid succession
                true   // Geographic anomaly
        );

        FraudAssessment assessment = engine.evaluate(context);

        // Should have 5 rules triggered (all except micro transfer)
        assertEquals(5, assessment.getTriggeredRules().size());
        assertTrue(assessment.shouldBlock());
        assertEquals("CRITICAL", assessment.getRiskScore().getRiskLevel());
    }

    @Test
    void shouldCapRiskScoreAtOne() {
        Money amount = new Money(BigDecimal.valueOf(100000), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-130",
                amount,
                2,
                true,
                true,
                true
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getRiskScore().getScore().compareTo(BigDecimal.ONE) <= 0);
    }

    @Test
    void shouldNotTriggerRulesForLowRiskTransfer() {
        Money amount = new Money(BigDecimal.valueOf(500), Currency.getInstance("USD"));
        FraudRulesEngine.FraudEvaluationContext context = new FraudRulesEngine.FraudEvaluationContext(
                "transfer-131",
                amount,
                10,  // Normal time
                false, // Existing destination
                false, // Not rapid succession
                false  // No geographic anomaly
        );

        FraudAssessment assessment = engine.evaluate(context);

        assertTrue(assessment.getTriggeredRules().isEmpty());
        assertFalse(assessment.shouldBlock());
        assertEquals("LOW", assessment.getRiskScore().getRiskLevel());
    }

    @Test
    void shouldThrowOnInvalidHourOfDay() {
        Money amount = new Money(BigDecimal.valueOf(100), Currency.getInstance("USD"));

        assertThrows(IllegalArgumentException.class, () -> {
            new FraudRulesEngine.FraudEvaluationContext(
                    "transfer-132",
                    amount,
                    25,  // Invalid hour
                    false,
                    false,
                    false
            );
        });
    }
}
