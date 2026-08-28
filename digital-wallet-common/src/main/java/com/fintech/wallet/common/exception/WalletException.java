package com.fintech.wallet.common.exception;

public sealed class WalletException extends RuntimeException permits
        DomainException,
        ApplicationException,
        InfrastructureException {

    private final String errorCode;
    private final String details;

    public WalletException(String message, String errorCode, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public WalletException(String message, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}
