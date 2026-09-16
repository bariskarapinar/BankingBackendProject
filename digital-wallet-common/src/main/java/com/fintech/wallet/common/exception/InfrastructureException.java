package com.fintech.wallet.common.exception;

public final class InfrastructureException extends WalletException {
    public InfrastructureException(String message, String errorCode, String details) {
        super(message, errorCode, details);
    }

    public InfrastructureException(String message, String errorCode, String details, Throwable cause) {
        super(message, errorCode, details, cause);
    }
}
