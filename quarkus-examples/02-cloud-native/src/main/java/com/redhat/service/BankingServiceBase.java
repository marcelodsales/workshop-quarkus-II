package com.redhat.service;

import com.redhat.exception.AccountAlreadyExistsException;
import com.redhat.exception.AccountNotFoundException;
import com.redhat.exception.InsufficientBalanceException;
import com.redhat.model.Account;
import com.redhat.model.Transaction;
import com.redhat.model.TransactionType;
import com.redhat.repository.AccountRepository;
import com.redhat.repository.TransactionRepository;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@ApplicationScoped
public class BankingServiceBase implements BankingService {

    @Inject
    AccountRepository accountRepository;

    @Inject
    TransactionRepository transactionRepository;

    public List<Account> getAllAccounts() {
        return accountRepository.findAll(PageRequest.ofSize(100), Order.by(Sort.asc("accountNumber"))).content();
    }

    @Transactional
    public Account createAccount(@NotEmpty String accountNumber, @NotEmpty String ownerId, @NotNull @PositiveOrZero BigDecimal initialBalance) {
        validateAccountExists(accountNumber);

        Account account = Account.builder().accountNumber(accountNumber).ownerId(ownerId).balance(initialBalance).build();
        accountRepository.save(account);
        return account;

    }

    @Transactional
    public Account deposit(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount) {

        Account account = findAccountByAccountNumber(accountNumber);
        account.deposit(amount);
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .accountNumber(account.getAccountNumber())
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .description("Deposit").build());

        return account;
    }

    @Transactional
    public Account withdraw(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount) {
        Account account = findAccountByAccountNumber(accountNumber);
        account.withDraw(amount);
        accountRepository.save(account);
        transactionRepository.save(Transaction.builder()
                .accountNumber(account.getAccountNumber())
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .description("Withdraw").build());

        return account;
    }

    @Transactional
    public void transfer(@NotEmpty String sourceAccountNumber, @NotEmpty String targetAccountNumber, @DecimalMin("0.01") BigDecimal amount) {

        Account sourceAccount = findAccountByAccountNumber(sourceAccountNumber);
        Account targetAccount = findAccountByAccountNumber(targetAccountNumber);

        if (!sourceAccount.hasAvailableBalance(amount)) {
            throw InsufficientBalanceException.builder().build();
        }

        sourceAccount.withDraw(amount);
        accountRepository.save(sourceAccount);
        transactionRepository.save(Transaction.builder()
                .accountNumber(sourceAccount.getAccountNumber())
                .type(TransactionType.TRANSFER_OUT)
                .amount(amount)
                .description("Transfer to " + targetAccount.getAccountNumber()).build());

        targetAccount.deposit(amount);
        accountRepository.save(targetAccount);
        transactionRepository.save(Transaction.builder()
                .accountNumber(targetAccount.getAccountNumber())
                .type(TransactionType.TRANSFER_IN)
                .amount(amount)
                .description("Transfer from " + sourceAccount.getAccountNumber()).build());

    }

    public BigDecimal getBalance(@NotEmpty String accountNumber) {
        Account account = findAccountByAccountNumber(accountNumber);
        return account.getBalance();
    }

    public List<Transaction> getTransactions(@NotEmpty String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    private Account findAccountByAccountNumber(String accountNumber) {
        return (Account) accountRepository.findById(accountNumber).orElseThrow(() -> AccountNotFoundException.builder().accountNumber(accountNumber).build());
    }

    private void validateAccountExists(String accountNumber) {
        if (accountRepository.countByAccountNumber(accountNumber) > 0) {
            throw AccountAlreadyExistsException.builder()
                    .accountNumber(accountNumber)
                    .build();
        }
    }
}
