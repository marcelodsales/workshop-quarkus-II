package com.demo.banking.service;

import com.demo.banking.entity.Account;
import com.demo.banking.entity.Transaction;
import com.demo.banking.exception.AccountAlreadyExistsException;
import com.demo.banking.exception.AccountNotFoundException;
import com.demo.banking.exception.InsufficientBalanceException;
import com.demo.banking.exception.InvalidAmountException;
import com.demo.banking.repository.AccountRepository;
import com.demo.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, 
                         TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Account createAccount(String accountNumber, String ownerId, BigDecimal initialBalance) {
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new AccountAlreadyExistsException("Account number already exists");
        }
        Account account = new Account(accountNumber, ownerId);
        account.setBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    @Transactional
    public Account deposit(Long accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        transactionRepository.save(new Transaction(
            accountId, 
            Transaction.TransactionType.DEPOSIT, 
            amount, 
            "Deposit"
        ));
        
        return account;
    }

    @Transactional
    public Account withdraw(Long accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        
        transactionRepository.save(new Transaction(
            accountId, 
            Transaction.TransactionType.WITHDRAW, 
            amount, 
            "Withdraw"
        ));
        
        return account;
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        
        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Source account not found"));
        
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        transactionRepository.save(new Transaction(
            fromAccountId, 
            Transaction.TransactionType.TRANSFER_OUT, 
            amount, 
            "Transfer to " + toAccount.getAccountNumber()
        ));
        
        transactionRepository.save(new Transaction(
            toAccountId, 
            Transaction.TransactionType.TRANSFER_IN, 
            amount, 
            "Transfer from " + fromAccount.getAccountNumber()
        ));
    }

    public BigDecimal getBalance(Long accountId) {
        return accountRepository.findById(accountId)
            .map(Account::getBalance)
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    public List<Transaction> getTransactions(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found");
        }
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
    }
}

