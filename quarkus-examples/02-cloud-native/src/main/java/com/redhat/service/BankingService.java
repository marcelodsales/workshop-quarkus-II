package com.redhat.service;

import com.redhat.model.Account;
import com.redhat.model.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public interface BankingService {

    List<Account> getAllAccounts();

    Account createAccount(@NotEmpty String accountNumber, @NotEmpty String ownerId, @NotNull @PositiveOrZero BigDecimal initialBalance);

    Account deposit(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount);

    Account withdraw(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount);

    void transfer(@NotEmpty String sourceAccountNumber, @NotEmpty String targetAccountNumber, @DecimalMin("0.01") BigDecimal amount);

    BigDecimal getBalance(@NotEmpty String accountNumber);

    List<Transaction> getTransactions(@NotEmpty String accountNumber);

}
