package com.redhat.model;

import com.redhat.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String ownerId;

    public boolean hasAvailableBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public Account deposit(@DecimalMin("0.01") BigDecimal amount) {
        setBalance(getBalance().add(amount));
        return this;
    }

    public Account withDraw(@DecimalMin("0.01") BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw InsufficientBalanceException.builder().build();
        }

        setBalance(getBalance().subtract(amount));

        return this;
    }

}

