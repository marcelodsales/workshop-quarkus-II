package com.redhat.service;

import com.redhat.exception.AccountNotFoundException;
import com.redhat.exception.InsufficientBalanceException;
import com.redhat.model.Account;
import com.redhat.model.Transaction;
import com.redhat.model.TransactionType;
import com.redhat.monitoring.TrackDataAccess;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
@TrackDataAccess
public class BankingService {

    public List<Account> getAllAccounts() {
        return Account.findAll(Sort.ascending("accountNumber")).list();
    }

    @Transactional
    public Account createAccount(@NotEmpty String accountNumber, @NotEmpty String ownerId, @NotNull @PositiveOrZero BigDecimal initialBalance) {
        return Account.builder().build().createAccount(accountNumber, ownerId, initialBalance);
    }

    @Transactional
    public Account deposit(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount) {
        Account account = Account.builder().build().deposit(accountNumber, amount);
        Transaction.builder().build().createTransaction(account.getAccountNumber(), TransactionType.DEPOSIT, amount, "Deposit");
        return account;
    }

    @Transactional
    public Account withdraw(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount) {
        Account account = Account.builder().build().withdraw(accountNumber, amount);
        Transaction.builder().build().createTransaction(account.getAccountNumber(), TransactionType.WITHDRAW, amount, "Withdraw");
        return account;
    }

    @Transactional
    public void transfer(@NotEmpty String sourceAccountNumber, @NotEmpty String targetAccountNumber, @DecimalMin("0.01") BigDecimal amount) {

        Account sourceAccount = (Account) Account.findByIdOptional(sourceAccountNumber).orElseThrow(() -> AccountNotFoundException.builder().accountNumber(sourceAccountNumber).build());
        Account targetAccount = (Account) Account.findByIdOptional(targetAccountNumber).orElseThrow(() -> AccountNotFoundException.builder().accountNumber(targetAccountNumber).build());

        if (!sourceAccount.hasAvailableBalance(amount)) {
            throw InsufficientBalanceException.builder().build();
        }

        sourceAccount.withDraw(amount);
        Transaction.builder().build().createTransaction(sourceAccount.getAccountNumber(), TransactionType.TRANSFER_OUT, amount, "Transfer to " + targetAccount.getAccountNumber());

        targetAccount.deposit(amount);
        Transaction.builder().build().createTransaction(targetAccount.getAccountNumber(), TransactionType.TRANSFER_IN, amount, "Transfer from " + sourceAccount.getAccountNumber());

    }

    public BigDecimal getBalance(@NotEmpty String accountNumber) {
        Account account = (Account) Account.findByIdOptional(accountNumber)
                .orElseThrow(() -> AccountNotFoundException.builder().accountNumber(accountNumber).build());
        return account.getBalance();
    }

    public List<Transaction> getTransactions(@NotEmpty String accountNumber) {
        Account.findByIdOptional(accountNumber)
                .orElseThrow(() -> AccountNotFoundException.builder().accountNumber(accountNumber).build());
        return Transaction.builder().build().getTransactions(accountNumber);
    }

}
