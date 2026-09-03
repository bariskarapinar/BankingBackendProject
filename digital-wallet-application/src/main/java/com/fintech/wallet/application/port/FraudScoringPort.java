package com.fintech.wallet.application.port;

import com.fintech.wallet.domain.fraud.FraudAssessment;
import com.fintech.wallet.domain.fraud.FraudRulesEngine;

public interface FraudScoringPort {
    FraudAssessment scoreTransfer(FraudRulesEngine.FraudEvaluationContext context);
}
