package com.redhat.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_seq")
    @SequenceGenerator(
            name = "transaction_seq",
            sequenceName = "transaction_id_seq",
            allocationSize = 1,
            initialValue = 1
    )
    private Long transactionId;

    @Column(nullable = false, updatable = false)
    @NotEmpty
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    @DecimalMin("0.0")
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    @PastOrPresent
    private LocalDateTime timestamp = LocalDateTime.now();

    private String description;

    public void createTransaction(String accountNumber, TransactionType type, BigDecimal amount, String description) {
        Transaction transaction = Transaction.builder()
                .accountNumber(accountNumber)
                .amount(amount)
                .type(type)
                .description(description)
                .build();
        transaction.persist();
    }

    public List<Transaction> getTransactions(@NotEmpty String accountNumber) {
        return find("accountNumber", accountNumber).list();
    }

}

