package com.redhat.rest.dto;

import com.redhat.model.Transaction;
import com.redhat.model.TransactionType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Transaction details response")
public record TransactionResponse(
        @Schema(description = "Transaction identifier")
        Long transactionId,
        
        @Schema(description = "Account number")
        String accountNumber,
        
        @Schema(description = "Transaction type")
        TransactionType type,
        
        @Schema(description = "Transaction amount")
        BigDecimal amount,
        
        @Schema(description = "Transaction timestamp")
        LocalDateTime timestamp,
        
        @Schema(description = "Transaction description")
        String description
) {
    public TransactionResponse(Transaction transaction) {
        this(
                transaction.getTransactionId(),
                transaction.getAccountNumber(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getTimestamp(),
                transaction.getDescription()
        );
    }
}

