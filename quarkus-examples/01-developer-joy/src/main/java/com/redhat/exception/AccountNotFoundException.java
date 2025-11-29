package com.redhat.exception;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
public class AccountNotFoundException extends RuntimeException {
    private String accountNumber;
    public AccountNotFoundException(String accountNumber) {
        super(String.format("Account number %s not found", accountNumber));
    }
}

