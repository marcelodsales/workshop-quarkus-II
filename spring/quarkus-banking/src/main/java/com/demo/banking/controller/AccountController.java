package com.demo.banking.controller;

import com.demo.banking.dto.AccountRequest;
import com.demo.banking.dto.TransactionResponse;
import com.demo.banking.dto.TransferRequest;
import com.demo.banking.entity.Account;
import com.demo.banking.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountRequest request) {
        Account account = accountService.createAccount(
            request.getAccountNumber(), 
            request.getOwnerId(),
            request.getInitialBalance()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(
            @PathVariable Long id, 
            @RequestBody Map<String, BigDecimal> request) {
        Account account = accountService.deposit(id, request.get("amount"));
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(
            @PathVariable Long id, 
            @RequestBody Map<String, BigDecimal> request) {
        Account account = accountService.withdraw(id, request.get("amount"));
        return ResponseEntity.ok(account);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(
            request.getFromAccountId(), 
            request.getToAccountId(), 
            request.getAmount()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@PathVariable Long id) {
        BigDecimal balance = accountService.getBalance(id);
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id) {
        List<TransactionResponse> transactions = accountService.getTransactions(id)
            .stream()
            .map(TransactionResponse::new)
            .toList();
        return ResponseEntity.ok(transactions);
    }
}

