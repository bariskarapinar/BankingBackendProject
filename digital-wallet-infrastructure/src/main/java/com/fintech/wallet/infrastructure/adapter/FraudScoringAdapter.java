package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.port.FraudScoringPort;
import com.fintech.wallet.domain.fraud.FraudAssessment;
import com.fintech.wallet.domain.fraud.FraudRulesEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudScoringAdapter implements FraudScoringPort {
    private final FraudRulesEngine fraudRulesEngine;

    @Override
    public FraudAssessment scoreTransfer(FraudRulesEngine.FraudEvaluationContext context) {
        log.debug("Scoring transfer - Transfer ID: {}, Amount: {}", 
                context.getTransferId(), context.getTransferAmount().getAmount());
        
        FraudAssessment assessment = fraudRulesEngine.evaluate(context);
        
        log.info("Fraud assessment complete - Transfer: {}, Risk Level: {}, Score: {:.2f}, Triggered Rules: {}",
                context.getTransferId(),
                assessment.getRiskScore().getRiskLevel(),
                assessment.getRiskScore().getScore(),
                assessment.getTriggeredRules());
        
        return assessment;
    }
}
