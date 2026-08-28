package com.fintech.wallet.common.exception;

public final class ApplicationException extends WalletException {
    public ApplicationException(String message, String errorCode, String details) {
        super(message, errorCode, details);
    }

    public ApplicationException(String message, String errorCode, String details, Throwable cause) {
        super(message, errorCode, details, cause);
    }
}
