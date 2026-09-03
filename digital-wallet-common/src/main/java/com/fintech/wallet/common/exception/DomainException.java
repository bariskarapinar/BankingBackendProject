package com.fintech.wallet.common.exception;

public final class DomainException extends WalletException {
    public DomainException(String message, String errorCode, String details) {
        super(message, errorCode, details);
    }

    public DomainException(String message, String errorCode, String details, Throwable cause) {
        super(message, errorCode, details, cause);
    }
}
