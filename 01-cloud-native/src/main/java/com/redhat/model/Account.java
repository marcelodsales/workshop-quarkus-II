package com.redhat.model;

import com.redhat.exception.AccountAlreadyExistsException;
import com.redhat.exception.AccountNotFoundException;
import com.redhat.exception.InsufficientBalanceException;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.Optional;

@Entity
@Getter
@Setter
@Table(name = "accounts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends PanacheEntityBase {

    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String ownerId;

    public boolean hasAvailableBalance(BigDecimal amount) {
        return balance.compareTo(amount) < 0;
    }

    public Account createAccount(@NotEmpty String accountNumber, @NotEmpty String ownerId, @NotNull @PositiveOrZero BigDecimal initialBalance) {
        Optional<PanacheEntityBase> dbAccount = findByIdOptional(accountNumber);
        if (dbAccount.isPresent()) {
            throw new AccountAlreadyExistsException(accountNumber);
        }

        Account account = Account.builder().accountNumber(accountNumber).ownerId(ownerId).balance(initialBalance).build();
        account.persist();
        return account;
    }

    public Account deposit(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount) {
        Account account = findAccountByAccountNumber(accountNumber);
        return account.deposit(amount);
    }

    public Account deposit(@DecimalMin("0.01") BigDecimal amount) {
        setBalance(getBalance().add(amount));
        persist();

        return this;
    }

    public Account withdraw(@NotEmpty String accountNumber, @DecimalMin("0.01") BigDecimal amount) {
        Account account = findAccountByAccountNumber(accountNumber);
        return account.withDraw(amount);
    }

    public Account withDraw(@DecimalMin("0.01") BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw InsufficientBalanceException.builder().build();
        }

        setBalance(getBalance().subtract(amount));
        persist();

        return this;
    }

    private Account findAccountByAccountNumber(String accountNumber) {
        return (Account) findByIdOptional(accountNumber).orElseThrow(() -> AccountNotFoundException.builder().accountNumber(accountNumber).build());
    }

    public BigDecimal getBalance(@NotEmpty String accountNumber) {
        Account account = findAccountByAccountNumber(accountNumber);
        return account.getBalance();
    }

}

