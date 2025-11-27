package com.redhat.exception;

import lombok.Builder;

@Builder
public class AccountAlreadyExistsException extends RuntimeException {
    private String accountNumber;
    public AccountAlreadyExistsException(String accountNumber) {
        super(String.format("Account %s already exists", accountNumber));
    }
}
