package com.fintech.wallet.application.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class ResilienceConfiguration {

    /**
     * Circuit Breaker for External Payment Gateway
     * Opens after 50% failure rate in last 100 calls
     * Half-open state attempts 5 calls, closes if successful
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreaker externalPaymentGatewayCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("external-payment-gateway", config);
        
        // Log circuit breaker state changes
        registry.getEventPublisher()
                .onEntryAdded(event -> {
                    CircuitBreaker cb = event.getAddedEntry();
                    log.info("CircuitBreaker created: {}", cb.getName());
                })
                .onEntryRemoved(event -> {
                    log.info("CircuitBreaker removed: {}", event.getRemovedEntry().getName());
                });

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("CircuitBreaker state transition - {}: {} -> {}",
                        circuitBreaker.getName(), event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.error("CircuitBreaker error - {}: {}", 
                        circuitBreaker.getName(), event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("CircuitBreaker success - {}: duration: {}ms",
                        circuitBreaker.getName(), event.getElapsedDuration().toMillis()));

        return circuitBreaker;
    }

    /**
     * Retry Policy for Transient Failures
     * Retries up to 3 times with exponential backoff
     */
    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public Retry transientFailureRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(500, 2))
                .recordExceptions(
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class,
                        java.io.IOException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        Retry retry = registry.retry("transient-failure-retry", config);

        retry.getEventPublisher()
                .onRetry(event -> log.warn("Retry attempt - {}: attempt: {}, exception: {}",
                        retry.getName(), event.getNumberOfRetryAttempts(), 
                        event.getLastThrowable().getClass().getSimpleName()))
                .onSuccess(event -> log.debug("Retry success - {}: attempts: {}",
                        retry.getName(), event.getNumberOfRetryAttempts()));

        return retry;
    }

    /**
     * Provides facade for using circuit breaker patterns
     */
    @Bean
    public ResiliencePatternFacade resiliencePatternFacade(
            CircuitBreaker externalPaymentGatewayCircuitBreaker,
            Retry transientFailureRetry) {
        return new ResiliencePatternFacade(externalPaymentGatewayCircuitBreaker, transientFailureRetry);
    }
}
