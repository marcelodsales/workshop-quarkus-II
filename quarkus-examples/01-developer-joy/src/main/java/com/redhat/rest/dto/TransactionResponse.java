package com.redhat.rest.dto;

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
        String type,
        
        @Schema(description = "Transaction amount")
        BigDecimal amount,
        
        @Schema(description = "Transaction timestamp")
        LocalDateTime timestamp,
        
        @Schema(description = "Transaction description")
        String description
) {

}

