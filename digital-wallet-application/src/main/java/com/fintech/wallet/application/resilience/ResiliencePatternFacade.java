package com.fintech.wallet.application.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class ResiliencePatternFacade {
    private final CircuitBreaker externalPaymentGatewayCircuitBreaker;
    private final Retry transientFailureRetry;

    /**
     * Execute operation with circuit breaker protection
     */
    public <T> T executeWithCircuitBreaker(Supplier<T> operation, String operationName) {
        try {
            return externalPaymentGatewayCircuitBreaker.executeSupplier(operation);
        } catch (CircuitBreakerOpenException e) {
            log.error("Circuit breaker open for operation: {}", operationName);
            throw new RuntimeException("Circuit breaker is open for " + operationName, e);
        }
    }

    /**
     * Execute operation with retry policy (exponential backoff)
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        try {
            return Retry.decorateSupplier(transientFailureRetry, operation).get();
        } catch (Exception e) {
            log.error("All retry attempts exhausted for operation: {}", operationName, e);
            throw new RuntimeException("Failed after retries: " + operationName, e);
        }
    }

    /**
     * Combined: Circuit Breaker + Retry Pattern
     * Tries with retry first, then applies circuit breaker
     */
    public <T> T executeWithCombinedPattern(Supplier<T> operation, String operationName) {
        Supplier<T> retryableOperation = Retry.decorateSupplier(transientFailureRetry, operation);
        return externalPaymentGatewayCircuitBreaker.executeSupplier(retryableOperation);
    }

    public CircuitBreaker.State getCircuitBreakerState() {
        return externalPaymentGatewayCircuitBreaker.getState();
    }

    public io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics getMetrics() {
        return externalPaymentGatewayCircuitBreaker.getMetrics();
    }

    /**
     * Custom exception indicating circuit breaker is open
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
