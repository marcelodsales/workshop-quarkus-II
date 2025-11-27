package com.redhat.exception;

import lombok.Builder;

@Builder
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Insufficient balance");
    }
}

