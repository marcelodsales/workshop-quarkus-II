package com.demo.banking.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class AccountRequest {
    
    @NotBlank
    private String accountNumber;
    
    @NotBlank
    private String ownerId;
    
    private BigDecimal initialBalance;

    public AccountRequest() {}

    public AccountRequest(String accountNumber, String ownerId, BigDecimal initialBalance) {
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.initialBalance = initialBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}

